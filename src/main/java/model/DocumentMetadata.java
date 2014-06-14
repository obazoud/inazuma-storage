package model;

import com.hazelcast.core.PartitionAware;
import storage.messages.PersistDocumentMessage;

import java.io.Serializable;

public class DocumentMetadata implements PartitionAware, Serializable
{
	public static final int ID = 1;

	private final String userID;
	private final long created;
	private boolean read;

	public DocumentMetadata(final PersistDocumentMessage message)
	{
		this(message.getUserID(), message.getCreated(), false);
	}

	public DocumentMetadata(final String userID, final long created, final boolean read)
	{
		this.userID = userID;
		this.created = created;
		this.read = read;
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
