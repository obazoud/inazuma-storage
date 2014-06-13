package storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import model.DocumentMetadata;
import model.SerializedData;

import java.util.Collection;

class StorageLookupController
{
	private final HazelcastInstance hz;

	public StorageLookupController(final HazelcastInstance hz)
	{
		this.hz = hz;
	}

	void createDocument(final String userID, final Collection<DocumentMetadata> documentMetadataCollection)
	{
		final IMap<String, DocumentMetadata> documentMetadataMap = getMap(userID);

		for (final DocumentMetadata documentMetadata : documentMetadataCollection)
		{
			documentMetadataMap.put(documentMetadata.getKey(), documentMetadata);
		}
	}

	void destroyDocument(final String userID)
	{
		getMap(userID).destroy();
	}

	void addSerializedData(final SerializedData serializedData)
	{
		getMap(serializedData.getUserID()).set(serializedData.getKey(), new DocumentMetadata(serializedData));
	}

	void deleteByKey(final String userID, final String key)
	{
		getMap(userID).delete(key);
	}

	String getDocumentKeysByUserID(final String userID)
	{
		return getMap(userID).keySet().toString();
	}

	Collection<DocumentMetadata> getDocumentMetadataCollection(final String userID)
	{
		return getMap(userID).values();
	}

	private IMap<String, DocumentMetadata> getMap(final String userID)
	{
		return hz.getMap("lookup-" + userID);
	}
}
