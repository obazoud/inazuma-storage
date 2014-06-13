package model;

import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

public class SerializedData implements StatusMessageObject, PartitionAware, Serializable
{
	public static final int ID = 2;

	private final String userID;
	private final String key;
	private final String document;
	private final long created;

	private int tries;
	private Exception lastException;

	public SerializedData(final String userID, final String key, final String document, final long created)
	{
		this.userID = userID;
		this.key = key;
		this.document = document;
		this.created = created;

		tries = 0;
		lastException = null;
	}

	public String getUserID()
	{
		return userID;
	}

	public String getKey()
	{
		return key;
	}

	public String getDocument()
	{
		return document;
	}

	public long getCreated()
	{
		return created;
	}

	@Override
	public int getTries()
	{
		return tries;
	}

	@Override
	public void incrementTries()
	{
		this.tries++;
	}

	@Override
	public Exception getLastException()
	{
		return lastException;
	}

	@Override
	public void setLastException(Exception lastException)
	{
		this.lastException = lastException;
	}

	@Override
	public void resetStatus()
	{
		tries = 0;
		lastException = null;
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
