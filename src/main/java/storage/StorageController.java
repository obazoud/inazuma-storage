package storage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.couchbase.client.CouchbaseClient;
import com.hazelcast.core.HazelcastInstance;
import model.SerializedData;
import scala.concurrent.duration.Duration;
import stats.BasicStatisticValue;
import stats.CustomStatisticValue;
import stats.StatisticManager;
import storage.messages.PersistLookupDocumentMessage;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StorageController
{
	private final CouchbaseClient cb;
	private final StorageLookupController storageLookupController;
	private final StorageDBController storageDBController;
	private final ActorSystem actorSystem;
	private final ActorRef storageDispatcher;

	private final AtomicLong queueSize = new AtomicLong(0);

	private final BasicStatisticValue dataAdded = new BasicStatisticValue("StorageController", "dataAdded");
	private final BasicStatisticValue dataFetched = new BasicStatisticValue("StorageController", "dataFetched");
	private final BasicStatisticValue dataDeleted = new BasicStatisticValue("StorageController", "dataDeleted");

	private final BasicStatisticValue lookupRetries = new BasicStatisticValue("StorageController", "retriesLookup");
	private final BasicStatisticValue lookupPersisted = new BasicStatisticValue("StorageController", "persistedLookup");

	private final BasicStatisticValue dataRetries = new BasicStatisticValue("StorageController", "retriesData");
	private final BasicStatisticValue dataPersisted = new BasicStatisticValue("StorageController", "persistedData");

	public StorageController(final HazelcastInstance hz, final CouchbaseClient cb)
	{
		this.cb = cb;
		this.storageLookupController = new StorageLookupController(hz);
		this.actorSystem = ActorSystem.create("MySystem");
		this.storageDispatcher = StorageFactory.createStorageDispatcher(actorSystem, this);
		this.storageDBController = new StorageDBController(cb);

		final CustomStatisticValue queueSize = new CustomStatisticValue<>("StorageController", "queueSize", new StorageQueueSizeCollector(this));
		StatisticManager.getInstance().registerStatisticValue(queueSize);
	}

	public String getKeysByUserID(final String userID)
	{
		return storageLookupController.getKeysByUserID(userID);
	}

	public void addData(final SerializedData serializedData)
	{
		queueSize.incrementAndGet();
		dataAdded.increment();
		storageDispatcher.tell(serializedData, ActorRef.noSender());
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
		if (storageLookupController.deleteByKey(userID, key))
		{
			// TODO add new message to delete document from database
			storageDispatcher.tell(new PersistLookupDocumentMessage(userID), ActorRef.noSender());

			dataDeleted.increment();
		}
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

	StorageLookupController getLookupController()
	{
		return storageLookupController;
	}

	StorageDBController getStorageDBController()
	{
		return storageDBController;
	}

	void incrementQueueSize()
	{
		queueSize.incrementAndGet();
	}

	void incrementLookupRetries()
	{
		lookupRetries.increment();
	}

	void incrementLookupPersisted()
	{
		queueSize.decrementAndGet();
		lookupPersisted.increment();
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
}
