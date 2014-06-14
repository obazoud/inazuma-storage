package database;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder;

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
		final CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
		builder.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);
		builder.setOpTimeout(10000);
		builder.setOpQueueMaxBlockTime(5000);
		builder.setMaxReconnectDelay(1500);
		builder.setTimeoutExceptionThreshold(5000);

		final List<URI> uris = new LinkedList<>();
		uris.add(URI.create("http://127.0.0.1:8091/pools"));

		CouchbaseClient tmpClient = null;
		try
		{
			final CouchbaseConnectionFactory connectionFactory = builder.buildCouchbaseConnection(uris, "default", "");
			tmpClient = new CouchbaseClient(connectionFactory);
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
