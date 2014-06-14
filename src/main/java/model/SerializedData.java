package model;

import com.hazelcast.core.PartitionAware;
import storage.messages.UserIDMessage;

import java.io.Serializable;

public class SerializedData extends UserIDMessage implements PartitionAware, Serializable
{
	public static final int ID = 2;

	private final String key;
	private final String document;
	private final long created;

	public SerializedData(final String userID, final String key, final String document, final long created)
	{
		super(userID);
		this.key = key;
		this.document = document;
		this.created = created;
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
	public Object getPartitionKey()
	{
		return getUserID();
	}
}
