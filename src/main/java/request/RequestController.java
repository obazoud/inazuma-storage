package request;

import main.Main;
import model.SerializedData;
import stats.BasicStatisticValue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class RequestController
{
	private final ExecutorService es;

	private final BasicStatisticValue dataAddedRequest = new BasicStatisticValue("RequestController", "dataAddedRequest");
	private final BasicStatisticValue dataFetchedRequest = new BasicStatisticValue("RequestController", "dataFetchedRequest");

	public RequestController(ExecutorService es)
	{
		this.es = es;
	}

	public String getKeys(final String userID)
	{
		try
		{
			final GetKeysFromLookupTask task = new GetKeysFromLookupTask(userID);
			Future<String> future = es.submit(task);
			return future.get();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void addData(final SerializedData serializedData)
	{
		dataAddedRequest.increment();
		final AddDataTask task = new AddDataTask(serializedData);
		es.submit(task);
	}

	public String getData(final String key)
	{
		dataFetchedRequest.increment();
		return Main.getStorageController().getData(key);
	}

	public void shutdown()
	{
	}
}
