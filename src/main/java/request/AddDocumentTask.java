package request;

import com.hazelcast.core.PartitionAware;
import main.Main;
import storage.messages.PersistDocumentMessage;

import java.io.Serializable;

class AddDocumentTask implements Runnable, PartitionAware, Serializable
{
	private final PersistDocumentMessage document;

	public AddDocumentTask(final PersistDocumentMessage document)
	{
		this.document = document;
	}

	@Override
	public void run()
	{
		Main.getStorageController().addDocument(document);
	}

	@Override
	public Object getPartitionKey()
	{
		return document.getPartitionKey();
	}
}
