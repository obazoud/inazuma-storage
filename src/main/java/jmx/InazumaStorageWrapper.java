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
	private static final IntObjectOpenHashMap<String> MAILS = new IntObjectOpenHashMap<>();
	private static final Random generator = new Random();

	static
	{
		for (int userID = 1; userID <= MAX_USER; userID++)
		{
			MAILS.put(userID, "{\"content\":" + userID + "}");
		}
	}

	@Override
	public void insertSingleDocumentForUser(final int userID)
	{
		final SerializedData serializedData = createSerializedDataForUser(userID);
		Main.getRequestController().addData(serializedData);
	}

	@Override
	public void insertSingleDocument()
	{
		final SerializedData serializedData = createSerializedDataForUser(createRandomUserID());
		Main.getRequestController().addData(serializedData);
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
		return returnKeys(String.valueOf(createRandomUserID()));
	}

	@Override
	public String returnKeys(final String userID)
	{
		return Main.getRequestController().getKeys(userID);
	}

	@Override
	public String returnData(final String key)
	{
		return Main.getRequestController().getData(key);
	}

	private int createRandomUserID()
	{
		return generator.nextInt(MAX_USER) + 1;
	}

	private SerializedData createSerializedDataForUser(final int userID)
	{
		final long created = (System.currentTimeMillis() / 1000) - generator.nextInt(86400);
		return new SerializedData(String.valueOf(userID), created, UUID.randomUUID().toString(), MAILS.get(userID));
	}
}
