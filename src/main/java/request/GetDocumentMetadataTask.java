package request;

import com.hazelcast.core.PartitionAware;
import main.Main;

import java.io.Serializable;
import java.util.concurrent.Callable;

class GetDocumentMetadataTask implements Callable<String>, PartitionAware, Serializable
{
	private final String userID;

	public GetDocumentMetadataTask(final String userID)
	{
		this.userID = userID;
	}

	@Override
	public String call() throws Exception
	{
		return Main.getStorageController().getDocumentMetadataByUserID(userID);
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
