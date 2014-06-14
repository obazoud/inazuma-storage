package storage;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import storage.messages.ProcessorIdleMessage;
import storage.messages.UserIDMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class StorageDispatcher extends UntypedActor
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
		if (message instanceof ProcessorIdleMessage)
		{
			dispatch((ProcessorIdleMessage) message);
		}
		else if (message instanceof UserIDMessage)
		{
			dispatch(((UserIDMessage) message).getUserID(), message);
		}
		else
		{
			unhandled(message);
		}
	}

	private void dispatch(final String userID, final Object message)
	{
		findOrCreateProcessorFor(userID).tell(message, self());
	}

	private void dispatch(final ProcessorIdleMessage message)
	{
		storageProcessorByUserID.remove(message.getUserID());

		sender().tell(PoisonPill.getInstance(), self());
	}

	private ActorRef findOrCreateProcessorFor(final String userID)
	{
		final ActorRef maybeActor = storageProcessorByUserID.get(userID);
		if (maybeActor != null)
		{
			return maybeActor;
		}

		final ActorRef storageProcessor = StorageActorFactory.createStorageProcessor(context(), storageController, userID);
		final ActorRef previousActor = storageProcessorByUserID.putIfAbsent(userID, storageProcessor);

		return (previousActor != null) ? previousActor : storageProcessor;
	}
}
