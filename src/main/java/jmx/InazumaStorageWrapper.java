package jmx;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import main.Main;
import storage.messages.DeleteDocumentMessage;
import storage.messages.PersistDocumentMessage;
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
		final PersistDocumentMessage message = createDocumentForUser(userID);
		Main.getRequestController().addDocument(message);
	}

	@Override
	public void insertSingleDocument()
	{
		insertSingleDocumentForUser(createRandomUserID());
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
	public String returnRandomDocumentMetadata()
	{
		return returnDocumentMetadata(String.valueOf(createRandomUserID()));
	}

	@Override
	public String returnDocumentMetadata(final String userID)
	{
		return Main.getRequestController().getDocumentMetadata(userID);
	}

	@Override
	public String returnDocument(final String key)
	{
		return Main.getRequestController().getDocument(key);
	}

	@Override
	public void deleteDocument(final String userID, final String key)
	{
		final DeleteDocumentMessage message = new DeleteDocumentMessage(userID, key);
		Main.getRequestController().deleteDocument(message);
	}

	private int createRandomUserID()
	{
		return generator.nextInt(MAX_USER) + 1;
	}

	private PersistDocumentMessage createDocumentForUser(final int userID)
	{
		final long created = (System.currentTimeMillis() / 1000) - generator.nextInt(86400);
		return new PersistDocumentMessage(String.valueOf(userID), UUID.randomUUID().toString(), MAILS.get(userID), created);
	}
}
