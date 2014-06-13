package storage;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Creator;

class StorageActorFactory
{
	public static ActorRef createStorageDispatcher(final ActorSystem context, final StorageController storageController)
	{
		return context.actorOf(Props.create(new Creator<StorageDispatcher>()
		{
			@Override
			public StorageDispatcher create() throws Exception
			{
				return new StorageDispatcher(storageController);
			}
		}), "storageDispatcher");
	}

	public static ActorRef createStorageProcessor(final ActorContext context, final StorageController storageController, final String userID)
	{
		return context.actorOf(Props.create(new Creator<StorageProcessor>()
		{
			@Override
			public StorageProcessor create() throws Exception
			{
				return new StorageProcessor(storageController, userID);
			}
		}));
	}
}
