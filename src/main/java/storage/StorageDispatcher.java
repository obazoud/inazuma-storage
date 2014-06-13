package storage;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import model.SerializedData;
import storage.messages.PersistLookupDocumentMessage;
import storage.messages.ProcessorIdleMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StorageDispatcher extends UntypedActor
{
	private final StorageController storageController;

	private final ConcurrentMap<String, ActorRef> storageProcessorByUserID = new ConcurrentHashMap<>();

	public StorageDispatcher(final StorageController storageController)
	{
		this.storageController = storageController;
	}

	@Override
	public void onReceive(Object message) throws Exception
	{
		if (message instanceof SerializedData)
		{
			dispatch(((SerializedData) message));
		}
		else if (message instanceof PersistLookupDocumentMessage)
		{
			dispatch((PersistLookupDocumentMessage) message);
		}
		else if (message instanceof ProcessorIdleMessage)
		{
			dispatch((ProcessorIdleMessage) message);
		}
		else
		{
			unhandled(message);
		}
	}

	private void dispatch(final SerializedData data)
	{
		findOrCreateProcessorFor(data.getUserID()).tell(data, self());
	}

	private void dispatch(final PersistLookupDocumentMessage data)
	{
		findOrCreateProcessorFor(data.getUserID()).tell(data, self());
	}

	private void dispatch(final ProcessorIdleMessage message)
	{
		storageProcessorByUserID.remove(message.getUserID());
		storageController.getLookupController().evictDocument(message.getUserID());

		sender().tell(PoisonPill.getInstance(), self());
	}

	private ActorRef findOrCreateProcessorFor(final String userID)
	{
		final ActorRef maybeActor = storageProcessorByUserID.get(userID);
		if (maybeActor != null)
		{
			return maybeActor;
		}

		final ActorRef storageProcessor = StorageFactory.createStorageProcessor(context(), storageController, userID);
		final ActorRef previousActor = storageProcessorByUserID.putIfAbsent(userID, storageProcessor);

		return (previousActor != null) ? previousActor : storageProcessor;
	}
}
