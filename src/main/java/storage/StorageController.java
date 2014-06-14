package storage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.couchbase.client.CouchbaseClient;
import com.hazelcast.core.HazelcastInstance;
import storage.messages.FetchDocumentMetadataMessage;
import storage.messages.PersistDocumentMessage;
import scala.concurrent.duration.Duration;
import stats.BasicStatisticValue;
import stats.CustomStatisticValue;
import stats.StatisticManager;
import storage.callbacks.FetchDocumentMetadataCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StorageController
{
	private final CouchbaseClient cb;
	private final StorageDocumentMetadataController storageDocumentMetadataController;
	private final StorageDBController storageDBController;
	private final ActorSystem actorSystem;
	private final ActorRef storageDispatcher;

	private final AtomicLong queueSize = new AtomicLong(0);

	private final BasicStatisticValue documentAdded = new BasicStatisticValue("StorageController", "documentAdded");
	private final BasicStatisticValue documentFetched = new BasicStatisticValue("StorageController", "documentFetched");
	private final BasicStatisticValue documentDeleted = new BasicStatisticValue("StorageController", "documentDeleted");

	private final BasicStatisticValue metadataRetries = new BasicStatisticValue("StorageController", "retriesMetadata");
	private final BasicStatisticValue metadataPersisted = new BasicStatisticValue("StorageController", "persistedMetadata");

	private final BasicStatisticValue documentRetries = new BasicStatisticValue("StorageController", "retriesDocument");
	private final BasicStatisticValue documentPersisted = new BasicStatisticValue("StorageController", "persistedDocument");

	private final BasicStatisticValue storageProcessorCreated = new BasicStatisticValue("StorageController", "processorsCreated");
	private final BasicStatisticValue storageProcessorDestroyed = new BasicStatisticValue("StorageController", "processorsDestroyed");

	public StorageController(final HazelcastInstance hz, final CouchbaseClient cb)
	{
		this.cb = cb;
		this.storageDocumentMetadataController = new StorageDocumentMetadataController(hz);
		this.actorSystem = ActorSystem.create("InazumaStorage");
		this.storageDispatcher = StorageActorFactory.createStorageDispatcher(actorSystem, this);
		this.storageDBController = new StorageDBController(cb);

		final CustomStatisticValue queueSize = new CustomStatisticValue<>("StorageController", "queueSize", new StorageQueueSizeCollector(this));
		StatisticManager.getInstance().registerStatisticValue(queueSize);
	}

	public String getDocumentMetadata(final String userID)
	{
		final FetchDocumentMetadataCallback callback = new FetchDocumentMetadataCallback();
		storageDispatcher.tell(new FetchDocumentMetadataMessage(userID, callback), ActorRef.noSender());

		return callback.getResult();
	}

	public void addDocument(final PersistDocumentMessage message)
	{
		queueSize.incrementAndGet();
		documentAdded.increment();
		storageDispatcher.tell(message, ActorRef.noSender());
	}

	public String getDocument(final String key)
	{
		try
		{
			documentFetched.increment();

			return String.valueOf(cb.get(key));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void deleteDocument(final String userID, final String key)
	{
		// TODO add new message to delete document from database
		//storageDispatcher.tell(new DeleteDocumentMessage(userID, key), ActorRef.noSender());

		documentDeleted.increment();
	}

	public long getQueueSize()
	{
		return queueSize.get();
	}

	public void shutdown()
	{
		try
		{
			actorSystem.shutdown();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void awaitShutdown()
	{
		actorSystem.awaitTermination(Duration.create(60, TimeUnit.MINUTES));
	}

	StorageDocumentMetadataController getStorageDocumentMetadataController()
	{
		return storageDocumentMetadataController;
	}

	StorageDBController getStorageDBController()
	{
		return storageDBController;
	}

	void incrementQueueSize()
	{
		queueSize.incrementAndGet();
	}

	void incrementMetadataRetries()
	{
		metadataRetries.increment();
	}

	void incrementMetadataPersisted()
	{
		queueSize.decrementAndGet();
		metadataPersisted.increment();
	}

	void incrementDataRetries()
	{
		documentRetries.increment();
	}

	void incrementDataPersisted()
	{
		queueSize.decrementAndGet();
		documentPersisted.increment();
	}

	void incrementStorageProcessorCreated()
	{
		storageProcessorCreated.increment();
	}

	void incrementStorageProcessorDestroyed()
	{
		storageProcessorDestroyed.increment();
	}
}
