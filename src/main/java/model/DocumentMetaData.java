package model;

import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

public class DocumentMetadata implements PartitionAware, Serializable
{
	public static final int ID = 1;

	private final String userID;
	private final String key;
	private final long created;
	private boolean read;

	public DocumentMetadata(final SerializedData serializedData)
	{
		this(serializedData.getUserID(), serializedData.getKey(), serializedData.getCreated(), false);
	}

	public DocumentMetadata(final String userID, final String key, final long created, final boolean read)
	{
		this.userID = userID;
		this.key = key;
		this.created = created;
		this.read = read;
	}

	public String getKey()
	{
		return key;
	}

	public long getCreated()
	{
		return created;
	}

	public boolean isRead()
	{
		return read;
	}

	public void setRead(final boolean read)
	{
		this.read = read;
	}

	@Override
	public Object getPartitionKey()
	{
		return userID;
	}
}
