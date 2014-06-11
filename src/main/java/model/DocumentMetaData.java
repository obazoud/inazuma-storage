package model;

import com.google.gson.annotations.Expose;
import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

public class DocumentMetadata implements PartitionAware, Serializable
{
	@Expose(serialize = false)
	private final String userID;

	private final String key;
	private final long created;

	private boolean read;

	public DocumentMetadata(final String userID, final String key, final long created)
	{
		this.userID = userID;
		this.key = key;
		this.created = created;
		this.read = false;
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
