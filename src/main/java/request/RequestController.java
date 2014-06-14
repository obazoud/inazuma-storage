package request;

import main.Main;
import storage.messages.PersistDocumentMessage;
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

	public String getDocumentMetadata(final String userID)
	{
		try
		{
			final GetDocumentMetadataTask task = new GetDocumentMetadataTask(userID);
			Future<String> future = es.submit(task);
			return future.get();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void addData(final PersistDocumentMessage message)
	{
		dataAddedRequest.increment();
		final AddDocumentTask task = new AddDocumentTask(message);
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
