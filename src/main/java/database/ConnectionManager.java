package database;

import com.couchbase.client.CouchbaseClient;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConnectionManager
{
	private static final CouchbaseClient client;

	static
	{
		// Set the URIs and get a client
		final List<URI> uris = new LinkedList<>();

		// Connect to localhost or to the appropriate URI(s)
		uris.add(URI.create("http://127.0.0.1:8091/pools"));

		CouchbaseClient tmpClient = null;
		try
		{
			tmpClient = new CouchbaseClient(uris, "default", "");
		}
		catch (IOException e)
		{
			System.err.println("IOException connecting to Couchbase: " + e.getMessage());
			System.exit(1);
		}
		finally
		{
			client = tmpClient;
		}
	}

	public static CouchbaseClient getConnection()
	{
		return client;
	}

	public static void shutdown()
	{
		client.shutdown(60, TimeUnit.SECONDS);
	}
}
