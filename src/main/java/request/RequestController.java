package request;

import main.Main;
import storage.messages.PersistDocumentMessage;
import stats.BasicStatisticValue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class RequestController
{
	private final ExecutorService es;

	private final BasicStatisticValue documentAddedRequest = new BasicStatisticValue("RequestController", "documentAddedRequest");
	private final BasicStatisticValue documentFetchedRequest = new BasicStatisticValue("RequestController", "documentFetchedRequest");

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

	public void addDocument(final PersistDocumentMessage message)
	{
		documentAddedRequest.increment();
		final AddDocumentTask task = new AddDocumentTask(message);
		es.submit(task);
	}

	public String getDocument(final String key)
	{
		documentFetchedRequest.increment();
		return Main.getStorageController().getDocument(key);
	}

	public void shutdown()
	{
	}
}
