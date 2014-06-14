package storage;

import com.couchbase.client.CouchbaseClient;
import net.spy.memcached.internal.OperationFuture;

import java.util.concurrent.ExecutionException;

class StorageDBController
{
	private final CouchbaseClient cb;

	public StorageDBController(final CouchbaseClient cb)
	{
		this.cb = cb;
	}

	public String getUserDocumentMetadata(final String userID)
	{
		return (String) cb.get(createDocumentMetadataKey(userID));
	}

	public void storeDocumentMetadata(final String userID, final String document) throws ExecutionException, InterruptedException
	{
		final OperationFuture<Boolean> future = cb.set(createDocumentMetadataKey(userID), 0, document);
		future.get();
	}

	public void storeDocument(final String key, final String document) throws ExecutionException, InterruptedException
	{
		final OperationFuture<Boolean> future = cb.set(key, 0, document);
		future.get();
	}

	public void deleteDocument(final String key) throws ExecutionException, InterruptedException
	{
		final OperationFuture<Boolean> future = cb.delete(key);
		future.get();
	}

	private String createDocumentMetadataKey(final String userID)
	{
		return "u-" + userID;
	}
}
