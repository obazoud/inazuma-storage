package storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import model.DocumentMetadata;

import java.util.Collection;

class StorageDocumentMetadataController
{
	private final HazelcastInstance hz;

	public StorageDocumentMetadataController(final HazelcastInstance hz)
	{
		this.hz = hz;
	}

	void createDocumentMetadata(final String userID, final Collection<DocumentMetadata> documentMetadataCollection)
	{
		final IMap<String, DocumentMetadata> documentMetadataMap = getMap(userID);

		for (final DocumentMetadata documentMetadata : documentMetadataCollection)
		{
			documentMetadataMap.put(documentMetadata.getKey(), documentMetadata);
		}
	}

	void destroyDocumentMetadata(final String userID)
	{
		getMap(userID).destroy();
	}

	void addDocumentMetadata(final String userID, final DocumentMetadata documentMetadata)
	{
		getMap(userID).set(userID, documentMetadata);
	}

	Collection<DocumentMetadata> getDocumentMetadataCollection(final String userID)
	{
		return getMap(userID).values();
	}

	void deleteDocument(final String userID, final String key)
	{
		getMap(userID).delete(key);
	}

	private IMap<String, DocumentMetadata> getMap(final String userID)
	{
		return hz.getMap("doc-meta-" + userID);
	}
}
