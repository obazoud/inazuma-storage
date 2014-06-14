package storage;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.DocumentMetadata;
import scala.concurrent.duration.Duration;
import serialization.GsonController;
import storage.messages.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class StorageProcessor extends UntypedActor
{
	private static final long DELAY = 50;
	private static final TimeUnit DELAY_UNIT = TimeUnit.MILLISECONDS;

	private final StorageController storageController;
	private final String userID;
	private final GsonController gson;

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private boolean isReady = false;
	private boolean persistDocumentMetadataMessageInQueue = false;
	private Map<String, DocumentMetadata> documentMetadataMap = new HashMap<>();

	public StorageProcessor(final StorageController storageController, final String userID)
	{
		this.storageController = storageController;
		this.userID = userID;
		this.gson = new GsonController(userID);

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
		if (message instanceof ReceiveTimeout)
		{
			processReceivedTimeout();
			return;
		}
		else if (message instanceof LoadDocumentMetadataMessage)
		{
			processLoadDocumentMetadataMessage((LoadDocumentMetadataMessage) message);
			return;
		}
		else if (!isReady)
		{
			sendDelayedMessage(message);
			return;
		}

		if (message instanceof PersistDocumentMetadataMessage)
		{
			processPersistDocumentMetadata((PersistDocumentMetadataMessage) message);
		}
		else if (message instanceof FetchDocumentMetadataMessage)
		{
			processFetchDocumentMetadata((FetchDocumentMetadataMessage) message);
		}
		else if (message instanceof PersistDocumentMessage)
		{
			processPersistDocument((PersistDocumentMessage) message);
		}
		else if (message instanceof FetchDocumentMessage)
		{
			processFetchDocument((FetchDocumentMessage) message);
		}
		else if (message instanceof DeleteDocumentMessage)
		{
			processDeleteDocument((DeleteDocumentMessage) message);
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

	private void processReceivedTimeout()
	{
		isReady = false;
		documentMetadataMap.clear();
		storageController.incrementStorageProcessorDestroyed();

		context().parent().tell(new StorageProcessorIdleMessage(userID), self());
	}

	private void processLoadDocumentMetadataMessage(final LoadDocumentMetadataMessage message)
	{
		try
		{
			final String documentMetadataJSON = storageController.getStorageDBController().getUserDocumentMetadata(userID);
			if (documentMetadataJSON != null)
			{
				documentMetadataMap = gson.getDocumentMetadataMap(documentMetadataJSON);

				if (documentMetadataMap == null)
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

	private void processPersistDocumentMetadata(final PersistDocumentMetadataMessage message)
	{
		persistDocumentMetadataMessageInQueue = false;

		try
		{
			storageController.getStorageDBController().storeDocumentMetadata(userID, gson.toJson(documentMetadataMap));
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
		message.getCallback().setResult(gson.toJson(documentMetadataMap));
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

			storageController.incrementDocumentRetries();
			sendDelayedMessage(message);

			return;
		}

		final DocumentMetadata documentMetadata = new DocumentMetadata(message);
		documentMetadataMap.put(message.getKey(), documentMetadata);
		storageController.incrementDocumentPersisted();

		if (!persistDocumentMetadataMessageInQueue)
		{
			persistDocumentMetadataMessageInQueue = true;

			storageController.incrementQueueSize();
			context().parent().tell(new PersistDocumentMetadataMessage(userID), getSelf());
		}
	}

	private void processFetchDocument(final FetchDocumentMessage message)
	{
		final String document = storageController.getStorageDBController().getDocument(message.getKey());
		message.getCallback().setResult(document);

		storageController.incrementDocumentFetched();
	}

	private void processDeleteDocument(final DeleteDocumentMessage message)
	{
		try
		{
			storageController.getStorageDBController().deleteDocument(message.getKey());
			documentMetadataMap.remove(message.getKey());

			context().parent().tell(new PersistDocumentMetadataMessage(userID), getSelf());
		}
		catch (Exception e)
		{
			log.error("Could not delete document {} for user {}: {}", message.getKey(), userID, e.getMessage());

			storageController.incrementDocumentRetries();
			sendDelayedMessage(message);

			return;
		}

		storageController.incrementDataDeleted();
	}
}
