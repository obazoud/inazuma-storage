package controller;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import model.UserLookupDocument;
import model.SerializedData;
import model.StatusMessageObject;
import net.spy.memcached.internal.OperationFuture;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.couchbase.client.CouchbaseClient;

class StorageControllerQueueThread extends Thread
{
	private final static int RETRY_DELAY = 50;
	
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final BlockingQueue<SerializedData> incomingQueue;
	private final IntObjectOpenHashMap<UserLookupDocument> lookupMap;
	private final IntOpenHashSet receiverOnQueue;
	private final int threadNo;
	private final StorageController storageController;
	private final CouchbaseClient client;
	private final int maxRetries;
	
	protected StorageControllerQueueThread(final StorageController storageController, final int threadNo, final CouchbaseClient client, final int maxRetries)
	{
		super("StorageController-thread-" + threadNo);
		this.incomingQueue = new LinkedBlockingQueue<SerializedData>();
		this.lookupMap = new IntObjectOpenHashMap<UserLookupDocument>();
		this.receiverOnQueue = new IntOpenHashSet();
		this.threadNo = threadNo;
		this.storageController = storageController;
		this.client = client;
		this.maxRetries = maxRetries;
	}

	protected void addData(final SerializedData serializedData)
	{
		incomingQueue.add(serializedData);
	}
	
	protected void deleteData(final int receiverID, final String key)
	{
		incomingQueue.add(new DeleteData(receiverID, key));
	}

	protected String getKeys(final int receiverID)
	{
		final UserLookupDocument userLookupDocument = getLookup(receiverID);
		if (userLookupDocument == null)
		{
			return null;
		}
		return userLookupDocument.toString();
	}
	
	protected int size()
	{
		return incomingQueue.size();
	}
	
	protected void shutdown()
	{
		running.set(false);
		incomingQueue.add(new PoisonedSerializedData());
	}
	
	@Override
	public void run()
	{
		while (running.get() || incomingQueue.size() > 0)
		{
			try
			{
				final SerializedData serializedData = incomingQueue.take();
				processData(serializedData);
			}
			catch (InterruptedException e)
			{
				System.err.println("Could not take serialized data from queue: " + e.getMessage());
			}
		}
		storageController.countdown();
	}
	
	private void processData(final SerializedData serializedData)
	{
		final int receiverID = serializedData.getUserID();
		if (serializedData instanceof PersistLookupDocumentOnly)
		{
			// Persist lookup document
			if (receiverOnQueue.contains(receiverID))
			{
				if (!persistLookup(receiverID, createLookupDocumentKey(receiverID), lookupMap.get(receiverID)))
				{
					incomingQueue.add(serializedData);
				}
			}
		}
		else if (serializedData instanceof DeleteData)
		{
			// Delete serializedData
			final UserLookupDocument userLookupDocument = getLookup(receiverID);
			if (userLookupDocument == null || !deleteData(serializedData))
			{
				incomingQueue.add(serializedData);
			}
			else
			{
				removeDataFromReceiverLookupDocument(serializedData, userLookupDocument);
			}
		}
        if (!(serializedData instanceof PoisonedSerializedData))
        {
			// Persist serializedData if not PoisonedSerializedData (which just unblocks the queue so the thread can shutdown properly)
			final UserLookupDocument userLookupDocument = getLookup(receiverID);
			if (userLookupDocument == null || !persistData(serializedData))
			{
				incomingQueue.add(serializedData);
			}
			else
			{
				addDataToReceiverLookupDocument(serializedData, userLookupDocument);
			}
		}
	}
	
	private void addDataToReceiverLookupDocument(final SerializedData serializedData, final UserLookupDocument userLookupDocument)
	{
		final int receiverID = serializedData.getUserID();
		final String lookupDocumentKey = createLookupDocumentKey(receiverID);

		// Modify lookup document
		if (!userLookupDocument.add(serializedData.getCreated(), serializedData.getKey()))
		{
			System.err.println("#" + threadNo + " Could not add serialized data with same key for " + serializedData.getKey());
			return;
		}
		
		// Store lookup document
		if (!persistLookup(receiverID, lookupDocumentKey, userLookupDocument))
		{
			removeDataFromReceiverLookupDocument(serializedData, userLookupDocument);
		}
	}
	
	private void removeDataFromReceiverLookupDocument(final SerializedData serializedData, final UserLookupDocument userLookupDocument)
	{
		final int receiverID = serializedData.getUserID();
		
		// FIXME UnitTests fail when removing data from document, check what is happening!
		//userLookupDocument.remove(serializedData.getKey());
		if (!receiverOnQueue.contains(receiverID))
		{
			receiverOnQueue.add(receiverID);
			incomingQueue.add(new PersistLookupDocumentOnly(receiverID));
		}
	}
	
	private UserLookupDocument getLookup(final int receiverID)
	{
		final String lookupDocumentKey = createLookupDocumentKey(receiverID);
		UserLookupDocument userLookupDocument = lookupMap.get(receiverID);
		if (userLookupDocument != null)
		{
			return userLookupDocument;
		}
		int tries = 0;
		while (tries++ < maxRetries)
		{
			try
			{
				final Object receiverLookupDocumentObject = client.get(lookupDocumentKey);
				if (receiverLookupDocumentObject != null)
				{
					userLookupDocument = UserLookupDocument.fromJSON((String) receiverLookupDocumentObject);
				}
				else
				{
					userLookupDocument = new UserLookupDocument();
				}
				lookupMap.put(receiverID, userLookupDocument);
				return userLookupDocument;
			}
			catch (Exception e)
			{
				System.err.println("#" + threadNo + " Could not read lookup document for " + receiverID + ": " + e.getMessage());
				threadSleep(tries * RETRY_DELAY);
			}
		}
		return null;
	}
	
	private boolean persistLookup(final int receiverID, final String lookupDocumentKey, final UserLookupDocument userLookupDocument)
	{
		final String document = userLookupDocument.toJSON();
		int tries = 0;
		while (tries++ < maxRetries)
		{
			try
			{
				OperationFuture<Boolean> lookupFuture = client.set(lookupDocumentKey, 0, document);
				if (lookupFuture.get())
				{
					receiverOnQueue.remove(receiverID);
					printStatusMessage("#" + threadNo + " Lookup document for receiver " + receiverID + " successfully saved", userLookupDocument);
					storageController.incrementLookupPersisted();
					return true;
				}
			}
			catch (Exception e)
			{
				userLookupDocument.setLastException(e);
				System.err.println("#" + threadNo + " Could not set lookup document for receiver " + receiverID + ": " + e.getMessage());
				threadSleep(tries * RETRY_DELAY);
			}
		}
		storageController.incrementLookupRetries();
		userLookupDocument.incrementTries();
		threadSleep(RETRY_DELAY);
		return false;
	}
	
	private boolean persistData(final SerializedData serializedData)
	{
		final String key = createDocumentKey(serializedData.getKey());
		int tries = 0;
		while (tries++ < maxRetries)
		{
			try
			{
				OperationFuture<Boolean> dataFuture = client.set(key, 0, serializedData.getDocument());
				if (dataFuture.get())
				{
					printStatusMessage("#" + threadNo + " data " + key + " successfully saved", serializedData);
					storageController.incrementDataPersisted();
					return true;
				}
			}
			catch (Exception e)
			{
				serializedData.setLastException(e);
				System.err.println("#" + threadNo + " Could not add " + key + " for receiver " + serializedData.getUserID() + ": " + e.getMessage());
				threadSleep(tries * RETRY_DELAY);
			}
		}
		storageController.incrementDataRetries();
		serializedData.incrementTries();
		threadSleep(RETRY_DELAY);
		return false;
	}
	
	private boolean deleteData(final SerializedData serializedData)
	{
		// TODO implement delete functionality
		return false;
	}
	
	private String createDocumentKey(final String key)
	{
		return "data_" + key;
	}

	private String createLookupDocumentKey(final int receiverID)
	{
		return "receiver_" + receiverID;
	}
	
	private void printStatusMessage(String statusMessage, final StatusMessageObject statusMessageObject)
	{
		Boolean showMessage = false;
		if (statusMessageObject.getTries() > 0)
		{
			statusMessage += ", Tries: " + statusMessageObject.getTries();
			showMessage = true;
		}
		if (statusMessageObject.getLastException() != null)
		{
			statusMessage += ", Last Exception: " + statusMessageObject.getLastException().getMessage();
			showMessage = true;
		}
		if (showMessage)
		{
			System.out.println(statusMessage);
		}
		statusMessageObject.resetStatus();
	}

	private void threadSleep(final long milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException ignored)
		{
		}
	}
	
	private class PoisonedSerializedData extends SerializedData
	{
		public PoisonedSerializedData()
		{
			super(0, 0, null, null);
		}
	}
	
	private class PersistLookupDocumentOnly extends SerializedData
	{
		public PersistLookupDocumentOnly(final int receiverID)
		{
			super(receiverID, 0, null, null);
		}
	}
	
	private class DeleteData extends SerializedData
	{
		public DeleteData(final int receiverID, final String key)
		{
			super(receiverID, 0, key, null);
		}
	}
}
