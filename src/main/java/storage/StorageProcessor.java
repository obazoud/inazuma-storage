package storage;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import gson.GsonWrapper;
import model.DocumentMetadata;
import model.SerializedData;
import scala.concurrent.duration.Duration;
import storage.messages.CreateLookupDocumentMessage;
import storage.messages.PersistLookupDocumentMessage;
import storage.messages.ProcessorIdleMessage;

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
	private boolean lookupDocumentInQueue = false;
	private Collection<DocumentMetadata> documentMetadataCollection = new ArrayList<>();

	public StorageProcessor(final StorageController storageController, final String userID)
	{
		this.storageController = storageController;
		this.userID = userID;

		this.gson = new GsonWrapper(userID);

		processCreateLookupDocumentMessage(new CreateLookupDocumentMessage(userID));
	}

	@Override
	public void preStart() throws Exception
	{
		context().setReceiveTimeout(Duration.create(5, TimeUnit.MINUTES));
	}

	@Override
	public void onReceive(Object message) throws Exception
	{
		if (message instanceof CreateLookupDocumentMessage)
		{
			processCreateLookupDocumentMessage((CreateLookupDocumentMessage) message);
		}
		else if (message instanceof ReceiveTimeout)
		{
			isReady = false;
			documentMetadataCollection.clear();
			//storageController.getLookupController().destroyDocument(userID);
			storageController.incrementStorageProcessorDestroyed();

			context().parent().tell(new ProcessorIdleMessage(userID), self());
		}
		else if (!isReady)
		{
			context().system().scheduler().scheduleOnce(
					Duration.create(DELAY, DELAY_UNIT),
					context().parent(),
					message,
					context().system().dispatcher(),
					self()
			);
		}
		else if (message instanceof SerializedData)
		{
			processSerializedData((SerializedData) message);
		}
		else if (message instanceof PersistLookupDocumentMessage)
		{
			processLookupDocument((PersistLookupDocumentMessage) message);
		}
		else
		{
			unhandled(message);
		}
	}

	private void processSerializedData(final SerializedData serializedData)
	{
		final String userID = serializedData.getUserID();

		try
		{
			storageController.getStorageDBController().storeDocument(serializedData.getKey(), serializedData.getDocument());
		}
		catch (Exception e)
		{
			log.error("Could not add {} for user {}: {}", serializedData.getKey(), userID, e.getMessage());

			serializedData.incrementTries();
			serializedData.setLastException(e);

			storageController.incrementDataRetries();
			context().system().scheduler().scheduleOnce(
					Duration.create(DELAY, DELAY_UNIT),
					context().parent(),
					serializedData,
					context().system().dispatcher(),
					self()
			);

			return;
		}

		final DocumentMetadata documentMetadata = new DocumentMetadata(serializedData);
		documentMetadataCollection.add(documentMetadata);
		//storageController.getLookupController().addSerializedData(userID, documentMetadata);
		storageController.incrementDataPersisted();

		if (!lookupDocumentInQueue)
		{
			lookupDocumentInQueue = true;

			storageController.incrementQueueSize();
			context().parent().tell(new PersistLookupDocumentMessage(userID), getSelf());
		}
	}

	private void processLookupDocument(final PersistLookupDocumentMessage message)
	{
		lookupDocumentInQueue = false;

		final String userID = message.getUserID();
		final String lookupDocument = gson.toJson(documentMetadataCollection);

		try
		{
			storageController.getStorageDBController().storeLookupDocument(userID, lookupDocument);
		}
		catch (Exception e)
		{
			log.error("Could not set lookup document for user {}: {}", userID, e.getMessage());

			storageController.incrementLookupRetries();
			context().system().scheduler().scheduleOnce(
					Duration.create(DELAY, DELAY_UNIT),
					context().parent(),
					message,
					context().system().dispatcher(),
					self()
			);

			return;
		}

		storageController.incrementLookupPersisted();
	}

	private void processCreateLookupDocumentMessage(final CreateLookupDocumentMessage message)
	{
		try
		{
			//final Collection<DocumentMetadata> maybeCollection = storageController.getLookupController().getDocumentMetadataCollection(userID);
			//if (maybeCollection != null && maybeCollection.size() > 0)
			//{
			//	documentMetadataCollection.set(maybeCollection);
			//	isReady = true;
			//
			//	return;
			//}

			final String userLookupDocumentObject = storageController.getStorageDBController().getUserLookupDocument(userID);
			if (userLookupDocumentObject != null)
			{
				documentMetadataCollection = gson.getDocumentMetadataCollection(userLookupDocumentObject);
				//storageController.getLookupController().createDocument(userID, documentMetadataCollection);

				if (documentMetadataCollection == null)
				{
					throw new RuntimeException("Lookup document for user " + userID + " is null! " + userLookupDocumentObject);
				}
			}

			storageController.incrementStorageProcessorCreated();
			isReady = true;
		}
		catch (Exception e)
		{
			log.error("Could not create lookup document for user {}: {}", userID, e.getMessage());

			context().parent().tell(message, getSelf());
		}
	}
}
