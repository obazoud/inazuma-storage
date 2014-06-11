package storage;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import model.DocumentMetadata;
import model.SerializedData;

import java.util.Collection;
import java.util.HashSet;

class StorageLookupController
{
	private final HazelcastInstance hz;

	public StorageLookupController(final HazelcastInstance hz)
	{
		this.hz = hz;
	}

	void populateDocument(final String userID, final String json)
	{
		if (json != null)
		{
			final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");

			final Collection<DocumentMetadata> metadataCollection = StorageFactory.createDocumentMetadataCollection(json);
			for (DocumentMetadata documentMetadata : metadataCollection)
			{
				documentMetadataMultiMap.put(userID, documentMetadata);
			}
		}
	}

	void evictDocument(final String userID)
	{
		final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");

		documentMetadataMultiMap.remove(userID);
	}

	void addSerializedData(final SerializedData serializedData)
	{
		final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");

		documentMetadataMultiMap.put(serializedData.getUserID(), new DocumentMetadata(serializedData.getUserID(), serializedData.getKey(), serializedData.getCreated()));
	}

	boolean deleteByKey(final String userID, final String key)
	{
		final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");

		final Collection<DocumentMetadata> metadataCollection = documentMetadataMultiMap.get(userID);
		for (DocumentMetadata documentMetadata : metadataCollection)
		{
			if (documentMetadata.getKey().equals(key))
			{
				documentMetadataMultiMap.remove(userID, documentMetadata);

				return true;
			}
		}

		return false;
	}

	String getKeysByUserID(final String userID)
	{
		final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");
		final Collection<DocumentMetadata> documentMetadataCollection = documentMetadataMultiMap.get(userID);

		final HashSet<String> keys = new HashSet<>();
		for (DocumentMetadata documentMetadata : documentMetadataCollection)
		{
			keys.add(documentMetadata.getKey());
		}

		return keys.toString();
	}

	String getJSONDocument(final String userID)
	{
		final MultiMap<String, DocumentMetadata> documentMetadataMultiMap = hz.getMultiMap("lookup");
		final Collection<DocumentMetadata> documentMetadataCollection = documentMetadataMultiMap.get(userID);

		return new Gson().toJson(documentMetadataCollection);
	}
}
