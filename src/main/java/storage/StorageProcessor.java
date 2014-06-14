package storage;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import gson.GsonWrapper;
import model.DocumentMetadata;
import storage.messages.*;
import storage.messages.PersistDocumentMessage;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

class StorageProcessor extends UntypedActor
{
	private static final long DELAY = 50;
	private static final TimeUnit DELAY_UNIT = TimeUnit.MILLISECONDS;

	private final StorageController storageController;
	private final String userID;
	private final GsonWrapper gson;

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private boolean isReady = false;
	private boolean PeristDocumentMetadataMessageInQueue = false;
	private Collection<DocumentMetadata> documentMetadataCollection = new ArrayList<>();

	public StorageProcessor(final StorageController storageController, final String userID)
	{
		this.storageController = storageController;
		this.userID = userID;
		this.gson = new GsonWrapper(userID);

		processLoadDocumentMetadataMessage(new LoadDocumentMetadataMessage(userID));
	}

	@Override
	public void preStart() throws Exception
	{
		context().setReceiveTimeout(Duration.create(5, TimeUnit.MINUTES));
	}

	@Override
	public void onReceive(final Object message) throws Exception
	{
		if (message instanceof LoadDocumentMetadataMessage)
		{
			processLoadDocumentMetadataMessage((LoadDocumentMetadataMessage) message);
		}
		else if (message instanceof ReceiveTimeout)
		{
			processReceivedTimeout();
		}
		else if (!isReady)
		{
			sendDelayedMessage(message);
		}
		else if (message instanceof PersistDocumentMessage)
		{
			processPersistDocument((PersistDocumentMessage) message);
		}
		else if (message instanceof PersistDocumentMetadataMessage)
		{
			processPersistDocumentMetadata((PersistDocumentMetadataMessage) message);
		}
		else if (message instanceof FetchDocumentMetadataMessage)
		{
			processFetchDocumentMetadata((FetchDocumentMetadataMessage) message);
		}
		else
		{
			unhandled(message);
		}
	}

	private void sendDelayedMessage(final Object message)
	{
		context().system().scheduler().scheduleOnce(
				Duration.create(DELAY, DELAY_UNIT),
				context().parent(),
				message,
				context().system().dispatcher(),
				self()
		);
	}

	private void processLoadDocumentMetadataMessage(final LoadDocumentMetadataMessage message)
	{
		try
		{
			//final Collection<DocumentMetadata> maybeCollection = storageController.getStorageDocumentMetadataController().getDocumentMetadataCollection(userID);
			//if (maybeCollection != null && maybeCollection.size() > 0)
			//{
			//	documentMetadataCollection.set(maybeCollection);
			//	isReady = true;
			//
			//	return;
			//}

			final String documentMetadataJSON = storageController.getStorageDBController().getUserDocumentMetadata(userID);
			if (documentMetadataJSON != null)
			{
				documentMetadataCollection = gson.getDocumentMetadataCollection(documentMetadataJSON);
				//storageController.getStorageDocumentMetadataController().createDocument(userID, documentMetadataCollection);

				if (documentMetadataCollection == null)
				{
					throw new RuntimeException("Document metadata for user " + userID + " is null! " + documentMetadataJSON);
				}
			}

			storageController.incrementStorageProcessorCreated();
			isReady = true;
		}
		catch (Exception e)
		{
			log.error("Could not create document metadata for user {}: {}", userID, e.getMessage());

			context().parent().tell(message, getSelf());
		}
	}

	private void processReceivedTimeout()
	{
		isReady = false;
		documentMetadataCollection.clear();
		//storageController.getStorageDocumentMetadataController().destroyDocument(userID);
		storageController.incrementStorageProcessorDestroyed();

		context().parent().tell(new StorageProcessorIdleMessage(userID), self());
	}

	private void processPersistDocument(final PersistDocumentMessage message)
	{
		try
		{
			storageController.getStorageDBController().storeDocument(message.getKey(), message.getDocument());
		}
		catch (Exception e)
		{
			log.error("Could not add {} for user {}: {}", message.getKey(), userID, e.getMessage());

			storageController.incrementDataRetries();
			sendDelayedMessage(message);

			return;
		}

		final DocumentMetadata documentMetadata = new DocumentMetadata(message);
		documentMetadataCollection.add(documentMetadata);
		//storageController.getStorageDocumentMetadataController().addDocumentMetadata(userID, documentMetadata);
		storageController.incrementDataPersisted();

		if (!PeristDocumentMetadataMessageInQueue)
		{
			PeristDocumentMetadataMessageInQueue = true;

			storageController.incrementQueueSize();
			context().parent().tell(new PersistDocumentMetadataMessage(userID), getSelf());
		}
	}

	private void processPersistDocumentMetadata(final PersistDocumentMetadataMessage message)
	{
		PeristDocumentMetadataMessageInQueue = false;

		try
		{
			storageController.getStorageDBController().storeDocumentMetadata(userID, gson.toJson(documentMetadataCollection));
		}
		catch (Exception e)
		{
			log.error("Could not store document metadata for user {}: {}", userID, e.getMessage());

			storageController.incrementMetadataRetries();
			sendDelayedMessage(message);

			return;
		}

		storageController.incrementMetadataPersisted();
	}

	private void processFetchDocumentMetadata(final FetchDocumentMetadataMessage message)
	{
		message.getCallback().setResult(gson.toJson(documentMetadataCollection));
	}
}
