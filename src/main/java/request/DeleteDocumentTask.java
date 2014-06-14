package request;

import com.hazelcast.core.PartitionAware;
import main.Main;
import storage.messages.DeleteDocumentMessage;

import java.io.Serializable;

public class DeleteDocumentTask implements Runnable, PartitionAware, Serializable
{
	private final DeleteDocumentMessage message;

	public DeleteDocumentTask(final DeleteDocumentMessage message)
	{
		this.message = message;
	}

	@Override
	public void run()
	{
		Main.getStorageController().deleteDocument(message);
	}

	@Override
	public Object getPartitionKey()
	{
		return message.getUserID();
	}
}
