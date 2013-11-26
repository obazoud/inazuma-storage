package controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import model.SerializedData;
import stats.BasicStatisticValue;
import stats.CustomStatisticValue;

import com.couchbase.client.CouchbaseClient;

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

		new CustomStatisticValue<Integer>("StorageService", "queueSize", new QueueSizeCollector(this));
	}

	public String getKeys(final int receiverID)
	{
		return threads[calculateThreadNumber(receiverID)].getKeys(receiverID);
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

	public void deleteData(final int receiverID, final String key)
	{
		dataDeleted.increment();
		threads[calculateThreadNumber(receiverID)].deleteData(receiverID, key);
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
		catch (InterruptedException e)
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

	private int calculateThreadNumber(final int receiverID)
	{
		return receiverID % numberOfThreads;
	}
}
