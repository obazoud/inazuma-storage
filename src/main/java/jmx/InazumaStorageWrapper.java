package jmx;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import main.Main;
import model.SerializedData;
import util.NamedThreadFactory;

import java.util.Random;
import java.util.UUID;

public class InazumaStorageWrapper implements InazumaStorageWrapperMBean
{
	private static final int MAX_USER = 100000;
	private static final IntObjectOpenHashMap<String> MAILS = new IntObjectOpenHashMap<String>();
	private static final Random generator = new Random();

	static
	{
		for (int userID = 1; userID <= MAX_USER; userID++)
		{
			MAILS.put(userID, "{\"content\":" + userID + "}");
		}
	}

	@Override
	public void insertSingleDocument()
	{
		final SerializedData serializedData = createRandomSerializedData();
		Main.getStorageController().addData(serializedData);
	}

	@Override
	public void insertThousandDocuments()
	{
		insertMultipleDocuments(1000);
	}

	@Override
	public void insertMultipleDocuments(final int count)
	{
		final NamedThreadFactory namedThreadFactory = new NamedThreadFactory("DocumentCreator");

		final Thread thread = namedThreadFactory.newThread(new Runnable()
		{
			@Override
			public void run()
			{
				int i = count;
				while (i-- > 0)
				{
					insertSingleDocument();
				}
			}
		});
		thread.start();
	}

	@Override
	public String returnRandomKeys()
	{
		return returnKeys(createRandomUserID());
	}

	@Override
	public String returnKeys(final int userID)
	{
		return Main.getStorageController().getKeys(userID);
	}

	@Override
	public String returnData(final String key)
	{
		return Main.getStorageController().getData(key);
	}

	private int createRandomUserID()
	{
		return generator.nextInt(MAX_USER) + 1;
	}

	private SerializedData createRandomSerializedData()
	{
		final int userID = createRandomUserID();
		final long created = (System.currentTimeMillis() / 1000) - generator.nextInt(86400);
		return new SerializedData(userID, created, UUID.randomUUID().toString(), MAILS.get(userID));
	}
}
