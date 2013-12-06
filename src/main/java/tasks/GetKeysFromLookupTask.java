package tasks;

import com.hazelcast.core.PartitionAware;
import main.Main;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class GetKeysFromLookupTask implements Callable<String>, PartitionAware, Serializable
{
	private final int userID;

	public GetKeysFromLookupTask(int userID)
	{
		this.userID = userID;
	}

	@Override
	public String call() throws Exception
	{
		return Main.getStorageController().getKeys(userID);
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
