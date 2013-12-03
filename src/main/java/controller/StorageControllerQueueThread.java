package controller;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.couchbase.client.CouchbaseClient;
import model.SerializedData;
import model.StatusMessageObject;
import model.UserLookupDocument;
import net.spy.memcached.internal.OperationFuture;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class StorageControllerQueueThread extends Thread
{
	private final static long RETRY_DELAY_MS = 50L;

	private final AtomicBoolean running = new AtomicBoolean(true);
	private final BlockingQueue<SerializedData> incomingQueue;
	private final IntObjectOpenHashMap<UserLookupDocument> lookupDocumentMap;
	private final IntOpenHashSet userOnQueue;
	private final int threadNo;
	private final StorageController storageController;
	private final CouchbaseClient client;
	private final int maxRetries;

	protected StorageControllerQueueThread(final StorageController storageController, final int threadNo, final CouchbaseClient client, final int maxRetries)
	{
		super("StorageController-thread-" + threadNo);
		this.incomingQueue = new LinkedBlockingQueue<SerializedData>();
		this.lookupDocumentMap = new IntObjectOpenHashMap<UserLookupDocument>();
		this.userOnQueue = new IntOpenHashSet();
		this.threadNo = threadNo;
		this.storageController = storageController;
		this.client = client;
		this.maxRetries = maxRetries;
	}

	protected void addData(final SerializedData serializedData)
	{
		incomingQueue.add(serializedData);
	}

	protected void deleteData(final int userID, final String key)
	{
		incomingQueue.add(new DeleteData(userID, key));
	}

	protected String getKeys(final int userID)
	{
		final UserLookupDocument userLookupDocument = getLookupDocument(userID);
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
		final int userID = serializedData.getUserID();
		if (serializedData instanceof PersistLookupDocumentOnly)
		{
			// Persist lookup document
			if (!persistLookup(userID, createLookupDocumentKey(userID), lookupDocumentMap.get(userID)))
			{
				incomingQueue.add(serializedData);
			}
			return;
		}
		else if (serializedData instanceof DeleteData)
		{
			// Delete serializedData
			final UserLookupDocument userLookupDocument = getLookupDocument(userID);
			if (userLookupDocument == null || !deleteData(serializedData))
			{
				incomingQueue.add(serializedData);
			}
			else
			{
				removeDataFromLookupDocument(serializedData, userLookupDocument);
			}
			return;
		}
		if (!(serializedData instanceof PoisonedSerializedData))
		{
			// Persist serializedData if not PoisonedSerializedData (which just unblocks the queue so the thread can shutdown properly)
			final UserLookupDocument userLookupDocument = getLookupDocument(userID);
			if (userLookupDocument == null || !persistData(serializedData))
			{
				incomingQueue.add(serializedData);
			}
			else
			{
				addDataToLookupDocument(serializedData, userLookupDocument);
			}
		}
	}

	private void addDataToLookupDocument(final SerializedData serializedData, final UserLookupDocument userLookupDocument)
	{
		// Modify lookup document
		if (!userLookupDocument.add(serializedData.getCreated(), serializedData.getKey()))
		{
			System.err.println("#" + threadNo + " Could not add serialized data with same key for " + serializedData.getKey());
			return;
		}

		storeLookupDocument(serializedData.getUserID());
	}

	private void removeDataFromLookupDocument(final SerializedData serializedData, final UserLookupDocument userLookupDocument)
	{
		// FIXME UnitTests fail when removing data from document, check what is happening!
		//userLookupDocument.remove(serializedData.getKey());

		storeLookupDocument(serializedData.getUserID());
	}

	private void storeLookupDocument(final int userID)
	{
		// Store lookup document
		if (!userOnQueue.contains(userID))
		{
			userOnQueue.add(userID);
			incomingQueue.add(new PersistLookupDocumentOnly(userID));
		}
	}

	private UserLookupDocument getLookupDocument(final int userID)
	{
		final String lookupDocumentKey = createLookupDocumentKey(userID);
		UserLookupDocument userLookupDocument = lookupDocumentMap.get(userID);
		if (userLookupDocument != null)
		{
			return userLookupDocument;
		}
		int tries = 0;
		while (tries++ < maxRetries)
		{
			try
			{
				final Object userLookupDocumentObject = client.get(lookupDocumentKey);
				if (userLookupDocumentObject != null)
				{
					userLookupDocument = UserLookupDocument.fromJSON((String) userLookupDocumentObject);
				}
				else
				{
					userLookupDocument = new UserLookupDocument();
				}
				lookupDocumentMap.put(userID, userLookupDocument);
				return userLookupDocument;
			}
			catch (Exception e)
			{
				System.err.println("#" + threadNo + " Could not read lookup document for " + userID + ": " + e.getMessage());
				threadSleep(tries);
			}
		}
		return null;
	}

	private boolean persistLookup(final int userID, final String lookupDocumentKey, final UserLookupDocument userLookupDocument)
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
					userOnQueue.remove(userID);
					printStatusMessage("#" + threadNo + " Lookup document for user " + userID + " successfully saved", userLookupDocument);
					storageController.incrementLookupPersisted();
					return true;
				}
			}
			catch (Exception e)
			{
				userLookupDocument.setLastException(e);
				System.err.println("#" + threadNo + " Could not set lookup document for user " + userID + ": " + e.getMessage());
				threadSleep(tries);
			}
		}
		storageController.incrementLookupRetries();
		userLookupDocument.incrementTries();
		threadSleep(1);
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
					printStatusMessage("#" + threadNo + " Data " + key + " successfully saved", serializedData);
					storageController.incrementDataPersisted();
					return true;
				}
			}
			catch (Exception e)
			{
				serializedData.setLastException(e);
				System.err.println("#" + threadNo + " Could not add " + key + " for user " + serializedData.getUserID() + ": " + e.getMessage());
				threadSleep(tries);
			}
		}
		storageController.incrementDataRetries();
		serializedData.incrementTries();
		threadSleep(1);
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

	private String createLookupDocumentKey(final int userID)
	{
		return "user_" + userID;
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

	private void threadSleep(final int tries)
	{
		try
		{
			TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * tries);
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
		public PersistLookupDocumentOnly(final int userID)
		{
			super(userID, 0, null, null);
		}
	}

	private class DeleteData extends SerializedData
	{
		public DeleteData(final int userID, final String key)
		{
			super(userID, 0, key, null);
		}
	}
}
