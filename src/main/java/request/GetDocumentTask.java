package request;

import com.hazelcast.core.PartitionAware;
import main.Main;

import java.io.Serializable;
import java.util.concurrent.Callable;

class GetDocumentTask implements Callable<String>, PartitionAware, Serializable
{
	private final String userID;
	private final String key;

	public GetDocumentTask(final String userID, final String key)
	{
		this.userID = userID;
		this.key = key;
	}

	@Override
	public String call() throws Exception
	{
		return Main.getStorageController().getDocument(userID, key);
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
