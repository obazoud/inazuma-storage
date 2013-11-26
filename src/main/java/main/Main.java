package main;

import com.couchbase.client.CouchbaseClient;
import controller.StorageController;
import database.ConnectionManager;
import jmx.JMXAgent;
import stats.StatisticManager;

import java.util.concurrent.CountDownLatch;

public class Main
{
    public static void main(String[] args)
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final CouchbaseClient client = ConnectionManager.getConnection();

        // Startup storage threads
        final StorageController storageController = new StorageController(client, Config.STORAGE_THREADS, Config.MAX_RETRIES);

        // Start JMX agent
        new JMXAgent();

        // Create shutdown event
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                System.out.println("Receiving shutdown signal...");
                shutdown(storageController, latch);
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

    private static void shutdown(final StorageController storageController, final CountDownLatch latch)
    {
        // Shutdown storage threads
        System.out.println("Shutting down StorageController...");
        storageController.shutdown();
        storageController.awaitShutdown();
        System.out.println("Done!\n");

        // Shutdown of connection manager
        System.out.println("Shutting down ConnectionManager...");
        ConnectionManager.shutdown();
        System.out.println("Done!\n");

        // Shutdown of StatisticManager
        System.out.println("Shutting down StatisticManager...");
        StatisticManager.getInstance().shutdown();
        System.out.println("Done!\n");

        // Release main thread
        latch.countDown();
    }
}
