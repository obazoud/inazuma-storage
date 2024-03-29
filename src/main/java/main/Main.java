package main;

import com.couchbase.client.CouchbaseClient;
import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import database.ConnectionManager;
import jmx.JMXAgent;
import model.DocumentMetadata;
import serialization.PersistDocumentMessageStreamSerializer;
import storage.messages.PersistDocumentMessage;
import serialization.DocumentMetadataStreamSerializer;
import request.RequestController;
import stats.StatisticManager;
import storage.StorageController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Main
{
	private static final AtomicReference<RequestController> requestControllerReference = new AtomicReference<>(null);
	private static final AtomicReference<StorageController> storageControllerReference = new AtomicReference<>(null);

	public static void main(String[] args)
	{
		final CountDownLatch latch = new CountDownLatch(1);

		// Get Hazelcast instance
		final SerializerConfig documentMetadataConfig = new SerializerConfig();
		documentMetadataConfig.setImplementation(new DocumentMetadataStreamSerializer()).setTypeClass(DocumentMetadata.class);

		final SerializerConfig persistDocumentConfig = new SerializerConfig();
		persistDocumentConfig.setImplementation(new PersistDocumentMessageStreamSerializer()).setTypeClass(PersistDocumentMessage.class);

		final Config cfg = new Config();
		cfg.getSerializationConfig()
				.addSerializerConfig(documentMetadataConfig)
				.addSerializerConfig(persistDocumentConfig);

		final HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);

		// Get Couchbase connection
		final CouchbaseClient cb = ConnectionManager.getConnection();

		// Start JMX agent
		new JMXAgent();

		// Startup request handler
		final RequestController requestController = new RequestController(hz.getExecutorService("executor"));
		requestControllerReference.set(requestController);

		// Startup storage threads
		final StorageController storageController = new StorageController(cb);
		storageControllerReference.set(storageController);

		// Create shutdown event
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("Receiving shutdown signal...");
				shutdown(requestController, storageController, latch);
			}
		}));

		// Wait for shutdown hook
		System.out.println("Inazuma-Storage is running...");
		try
		{
			latch.await();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.out.println("Inazuma-Storage is shut down!");
		System.exit(0);
	}

	public static RequestController getRequestController()
	{
		return requestControllerReference.get();
	}

	public static StorageController getStorageController()
	{
		return storageControllerReference.get();
	}

	private static void shutdown(final RequestController requestController, final StorageController storageController, final CountDownLatch latch)
	{
		// Shutdown request storage
		System.out.println("Shutting down RequestController...");
		requestController.shutdown();
		System.out.println("Done!\n");

		// Shutdown storage threads
		System.out.println("Shutting down StorageController...");
		storageController.shutdown();
		storageController.awaitShutdown();
		System.out.println("Done!\n");

		// Shutdown of connection manager
		System.out.println("Shutting down ConnectionManager...");
		ConnectionManager.shutdown();
		System.out.println("Done!\n");

		// Shutdown of Hazelcast instance
		System.out.println("Shutting down Hazelcast instance...");
		Hazelcast.shutdownAll();
		System.out.println("Done!\n");

		// Shutdown of StatisticManager
		System.out.println("Shutting down StatisticManager...");
		StatisticManager.getInstance().shutdown();
		System.out.println("Done!\n");

		// Release main thread
		latch.countDown();
	}
}
