package request;

import com.hazelcast.core.PartitionAware;
import main.Main;
import model.SerializedData;

import java.io.Serializable;

class AddDataTask implements Runnable, PartitionAware, Serializable
{
	private final String userID;
	private final SerializedData document;

	public AddDataTask(final SerializedData document)
	{
		this.userID = document.getUserID();
		this.document = document;
	}

	@Override
	public void run()
	{
		Main.getStorageController().addData(document);
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
