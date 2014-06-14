package storage;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import gson.GsonWrapper;
import model.SerializedData;
import scala.concurrent.duration.Duration;
import storage.messages.CreateLookupDocumentMessage;
import storage.messages.PersistLookupDocumentMessage;
import storage.messages.ProcessorIdleMessage;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

class StorageProcessor extends UntypedActor
{
	private final StorageController storageController;
	private final String userID;

	private final GsonWrapper gson;

	private final HashSet<String> lookupDocumentsInQueue = new HashSet<>();
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private boolean isReady = false;

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
			storageController.incrementStorageProcessorDestroyed();
			storageController.getLookupController().destroyDocument(userID);

			context().parent().tell(new ProcessorIdleMessage(userID), self());
		}
		else if (!isReady)
		{
			context().system().scheduler().scheduleOnce(
					Duration.create(50, TimeUnit.MILLISECONDS),
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
					Duration.create(50, TimeUnit.MILLISECONDS),
					context().parent(),
					serializedData,
					context().system().dispatcher(),
					self()
			);

			return;
		}

		storageController.getLookupController().addSerializedData(serializedData);
		storageController.incrementDataPersisted();

		if (!lookupDocumentsInQueue.contains(userID))
		{
			lookupDocumentsInQueue.add(userID);

			storageController.incrementQueueSize();
			context().parent().tell(new PersistLookupDocumentMessage(userID), getSelf());
		}
	}

	private void processLookupDocument(final PersistLookupDocumentMessage message)
	{
		lookupDocumentsInQueue.remove(userID);

		final String userID = message.getUserID();
		final String lookupDocument = gson.toJson(storageController.getLookupController().getDocumentMetadataCollection(userID));

		try
		{
			storageController.getStorageDBController().storeLookupDocument(userID, lookupDocument);
		}
		catch (Exception e)
		{
			log.error("Could not set lookup document for user {}: {}", userID, e.getMessage());

			storageController.incrementLookupRetries();
			context().system().scheduler().scheduleOnce(
					Duration.create(50, TimeUnit.MILLISECONDS),
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
			final String userLookupDocumentObject = storageController.getStorageDBController().getUserLookupDocument(userID);
			if (userLookupDocumentObject != null)
			{
				storageController.getLookupController().createDocument(userID, gson.getDocumentMetadataCollection(userLookupDocumentObject));
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
