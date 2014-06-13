package storage;

import com.couchbase.client.CouchbaseClient;
import net.spy.memcached.internal.OperationFuture;

import java.util.concurrent.ExecutionException;

public class StorageDBController
{
	private final CouchbaseClient cb;

	public StorageDBController(final CouchbaseClient cb)
	{
		this.cb = cb;
	}

	public String getUserLookupDocument(final String userID)
	{
		return (String) cb.get(createLookupDocumentKey(userID));
	}

	public void storeDocument(final String key, final String document) throws ExecutionException, InterruptedException
	{
		final OperationFuture<Boolean> dataFuture = cb.set(key, 0, document);
		dataFuture.get();
	}

	public void storeLookupDocument(final String userID, final String document) throws ExecutionException, InterruptedException
	{
		final OperationFuture<Boolean> lookupFuture = cb.set(createLookupDocumentKey(userID), 0, document);
		lookupFuture.get();
	}

	private String createLookupDocumentKey(final String userID)
	{
		return "u-" + userID;
	}
}
