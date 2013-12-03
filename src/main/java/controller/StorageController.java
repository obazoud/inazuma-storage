package controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import model.SerializedData;
import stats.BasicStatisticValue;
import stats.CustomStatisticValue;

import com.couchbase.client.CouchbaseClient;
import stats.StatisticManager;

public class StorageController
{
	private final CouchbaseClient client;
	private final int numberOfThreads;
	private final StorageControllerQueueThread[] threads;
	private final CountDownLatch latch;

	private final BasicStatisticValue dataFetched = new BasicStatisticValue("StorageService", "dataFetched");
	private final BasicStatisticValue dataAdded = new BasicStatisticValue("StorageService", "dataAdded");
	private final BasicStatisticValue dataDeleted = new BasicStatisticValue("StorageService", "dataDeleted");

	private final BasicStatisticValue lookupRetries = new BasicStatisticValue("StorageService", "retriesLookup");
	private final BasicStatisticValue lookupPersisted = new BasicStatisticValue("StorageService", "persistedLookup");

	private final BasicStatisticValue dataRetries = new BasicStatisticValue("StorageService", "retriesData");
	private final BasicStatisticValue dataPersisted = new BasicStatisticValue("StorageService", "persistedData");

	public StorageController(final CouchbaseClient client, final int numberOfThreads, final int maxRetries)
	{
		this.client = client;
		this.numberOfThreads = numberOfThreads;
		this.threads = new StorageControllerQueueThread[numberOfThreads];
		this.latch = new CountDownLatch(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++)
		{
			threads[i] = new StorageControllerQueueThread(this, i + 1, client, maxRetries);
			threads[i].start();
		}

		final CustomStatisticValue queueSize = new CustomStatisticValue<Integer>("StorageService", "queueSize", new QueueSizeCollector(this));
        StatisticManager.getInstance().registerStatisticValue(queueSize);
	}

	public String getKeys(final int userID)
	{
		return threads[calculateThreadNumber(userID)].getKeys(userID);
	}

	public String getData(final String key)
	{
		// TODO: Add exception handling
		dataFetched.increment();
		return String.valueOf(client.get("data_" + key));
	}

	public void addData(final SerializedData serializedData)
	{
		dataAdded.increment();
		threads[calculateThreadNumber(serializedData.getUserID())].addData(serializedData);
	}

	public void deleteData(final int userID, final String key)
	{
		dataDeleted.increment();
		threads[calculateThreadNumber(userID)].deleteData(userID, key);
	}

	public int size()
	{
		int size = 0;
		for (int i = 0; i < numberOfThreads; i++)
		{
			size += threads[i].size();
		}
		return size;
	}

	public void shutdown()
	{
		for (int i = 0; i < numberOfThreads; i++)
		{
			threads[i].shutdown();
		}
	}

	public void awaitShutdown()
	{
		try
		{
			latch.await(60, TimeUnit.MINUTES);
		}
		catch (InterruptedException ignored)
		{
		}
	}

	protected void incrementLookupRetries()
	{
		lookupRetries.increment();
	}

	protected void incrementLookupPersisted()
	{
		lookupPersisted.increment();
	}

	protected void incrementDataRetries()
	{
		dataRetries.increment();
	}

	protected void incrementDataPersisted()
	{
		dataPersisted.increment();
	}

	protected void countdown()
	{
		latch.countDown();
	}

	private int calculateThreadNumber(final int userID)
	{
		return userID % numberOfThreads;
	}
}
