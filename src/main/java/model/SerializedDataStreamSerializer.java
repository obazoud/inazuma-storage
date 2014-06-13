package model;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

public class SerializedDataStreamSerializer implements StreamSerializer<SerializedData>
{
	@Override
	public void write(final ObjectDataOutput out, final SerializedData object) throws IOException
	{
		out.writeUTF((String) object.getPartitionKey());
		out.writeUTF(object.getKey());
		out.writeUTF(object.getDocument());
		out.writeLong(object.getCreated());
	}

	@Override
	public SerializedData read(final ObjectDataInput in) throws IOException
	{
		return new SerializedData(in.readUTF(), in.readUTF(), in.readUTF(), in.readLong());
	}

	@Override
	public int getTypeId()
	{
		return SerializedData.ID;
	}

	@Override
	public void destroy()
	{
	}
}
