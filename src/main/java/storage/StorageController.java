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

	private final BasicStatisticValue dataAdded = new BasicStatisticValue("StorageController", "dataAdded");
	private final BasicStatisticValue dataFetched = new BasicStatisticValue("StorageController", "dataFetched");
	private final BasicStatisticValue dataDeleted = new BasicStatisticValue("StorageController", "dataDeleted");

	private final BasicStatisticValue metadataRetries = new BasicStatisticValue("StorageController", "retriesMetadata");
	private final BasicStatisticValue metadataPersisted = new BasicStatisticValue("StorageController", "persistedMetadata");

	private final BasicStatisticValue dataRetries = new BasicStatisticValue("StorageController", "retriesData");
	private final BasicStatisticValue dataPersisted = new BasicStatisticValue("StorageController", "persistedData");

	private final BasicStatisticValue storageProcessorCreated = new BasicStatisticValue("StorageController", "processorsCreated");
	private final BasicStatisticValue storageProcessorDestroyed = new BasicStatisticValue("StorageController", "processorsDestroyed");

	public StorageController(final HazelcastInstance hz, final CouchbaseClient cb)
	{
		this.cb = cb;
		this.storageDocumentMetadataController = new StorageDocumentMetadataController(hz);
		this.actorSystem = ActorSystem.create("MySystem");
		this.storageDispatcher = StorageActorFactory.createStorageDispatcher(actorSystem, this);
		this.storageDBController = new StorageDBController(cb);

		final CustomStatisticValue queueSize = new CustomStatisticValue<>("StorageController", "queueSize", new StorageQueueSizeCollector(this));
		StatisticManager.getInstance().registerStatisticValue(queueSize);
	}

	public String getDocumentMetadataByUserID(final String userID)
	{
		final FetchDocumentMetadataCallback callback = new FetchDocumentMetadataCallback();
		storageDispatcher.tell(new FetchDocumentMetadataMessage(userID, callback), ActorRef.noSender());

		return callback.getResult();
	}

	public void addData(final PersistDocumentMessage message)
	{
		queueSize.incrementAndGet();
		dataAdded.increment();
		storageDispatcher.tell(message, ActorRef.noSender());
	}

	public String getData(final String key)
	{
		try
		{
			dataFetched.increment();

			return String.valueOf(cb.get(key));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void deleteData(final String userID, final String key)
	{
		// TODO add new message to delete document from database
		//storageDispatcher.tell(new DeleteDocument(userID, key), ActorRef.noSender());

		dataDeleted.increment();
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
		dataRetries.increment();
	}

	void incrementDataPersisted()
	{
		queueSize.decrementAndGet();
		dataPersisted.increment();
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
