package storage;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.couchbase.client.CouchbaseClient;
import model.SerializedData;
import net.spy.memcached.internal.OperationFuture;
import scala.concurrent.duration.Duration;
import storage.messages.PersistLookupDocumentMessage;
import storage.messages.ProcessorIdleMessage;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class StorageProcessor extends UntypedActor
{
	private final CouchbaseClient cb;
	private final StorageController storageController;
	private final String userID;

	private final HashSet<String> lookupDocumentsInQueue = new HashSet<>();
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public StorageProcessor(final CouchbaseClient cb, final StorageController storageController, final String userID)
	{
		this.cb = cb;
		this.storageController = storageController;
		this.userID = userID;

		final Object userLookupDocumentObject = cb.get(createLookupDocumentKey(userID));
		storageController.getLookupController().populateDocument(userID, (String) userLookupDocumentObject);
	}

	@Override
	public void preStart() throws Exception
	{
		context().setReceiveTimeout(Duration.create(5, TimeUnit.MINUTES));
	}

	@Override
	public void onReceive(Object message) throws Exception
	{
		if (message instanceof SerializedData)
		{
			processSerializedData((SerializedData) message);
		}
		else if (message instanceof PersistLookupDocumentMessage)
		{
			processLookupDocument((PersistLookupDocumentMessage) message);
		}
		else if (message instanceof ReceiveTimeout)
		{
			context().parent().tell(new ProcessorIdleMessage(userID), self());
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
			OperationFuture<Boolean> dataFuture = cb.set(serializedData.getKey(), 0, serializedData.getDocument());
			if (dataFuture.get())
			{
				storageController.getLookupController().addSerializedData(serializedData);
				storageController.incrementDataPersisted();

				if (!lookupDocumentsInQueue.contains(userID))
				{
					lookupDocumentsInQueue.add(userID);

					storageController.incrementQueueSize();
					context().parent().tell(new PersistLookupDocumentMessage(userID), getSelf());
				}

				return;
			}
		}
		catch (Exception e)
		{
			log.error("Could not add {} for user {}: {}", serializedData.getKey(), userID, e.getMessage());

			serializedData.incrementTries();
			serializedData.setLastException(e);
		}

		storageController.incrementDataRetries();
		context().parent().tell(serializedData, getSelf());
	}

	private void processLookupDocument(final PersistLookupDocumentMessage message)
	{
		final String userID = message.getUserID();

		try
		{
			lookupDocumentsInQueue.remove(userID);

			OperationFuture<Boolean> lookupFuture = cb.set(createLookupDocumentKey(userID), 0, storageController.getLookupController().getJSONDocument(userID));
			if (lookupFuture.get())
			{
				storageController.incrementLookupPersisted();

				return;
			}
		}
		catch (Exception e)
		{
			log.error("Could not set lookup document for user {}: {}", userID, e.getMessage());
		}

		storageController.incrementLookupRetries();
		context().parent().tell(message, getSelf());
	}

	private String createLookupDocumentKey(final String userID)
	{
		return "u-" + userID;
	}
}
