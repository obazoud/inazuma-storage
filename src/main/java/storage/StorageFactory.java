package storage;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.DocumentMetadata;

import java.lang.reflect.Type;
import java.util.Collection;

class StorageFactory
{
	private static final Type typeOfCollection;

	static
	{
		TypeToken<Collection<DocumentMetadata>> typeToken = new TypeToken<Collection<DocumentMetadata>>()
		{
		};
		typeOfCollection = typeToken.getType();
	}

	public static Collection<DocumentMetadata> createDocumentMetadataCollection(final String json)
	{
		return new Gson().fromJson(json, typeOfCollection);
	}

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
