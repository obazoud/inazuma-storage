package model;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

public class SerializedData implements StatusMessageObject, IdentifiedDataSerializable, PartitionAware
{
	private int userID;
	private long created;
	private String key;
	private String document;

	private transient int tries;
	private transient Exception lastException;

	public SerializedData(final int userID, final long created, final String key, final String document)
	{
		this.userID = userID;
		this.created = created;
		this.key = key;
		this.document = document;

		tries = 0;
		lastException = null;
	}

	protected SerializedData()
	{
	}

	public int getUserID()
	{
		return userID;
	}

	public long getCreated()
	{
		return created;
	}

	public String getKey()
	{
		return key;
	}

	public String getDocument()
	{
		return document;
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

	@Override
	public int getFactoryId()
	{
		return SerializationFactory.SERIALIZED_DATA;
	}

	@Override
	public int getId()
	{
		return SerializationFactory.SERIALIZED_DATA;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException
	{
		out.writeInt(userID);
		out.writeLong(created);
		out.writeUTF(key);
		out.writeUTF(document);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException
	{
		userID = in.readInt();
		created = in.readLong();
		key = in.readUTF();
		document = in.readUTF();
	}
}
