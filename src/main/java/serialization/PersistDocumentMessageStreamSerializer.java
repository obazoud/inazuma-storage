package serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import storage.messages.PersistDocumentMessage;

import java.io.IOException;

public class PersistDocumentMessageStreamSerializer implements StreamSerializer<PersistDocumentMessage>
{
	@Override
	public void write(final ObjectDataOutput out, final PersistDocumentMessage object) throws IOException
	{
		out.writeUTF((String) object.getPartitionKey());
		out.writeUTF(object.getKey());
		out.writeUTF(object.getDocument());
		out.writeLong(object.getCreated());
	}

	@Override
	public PersistDocumentMessage read(final ObjectDataInput in) throws IOException
	{
		return new PersistDocumentMessage(in.readUTF(), in.readUTF(), in.readUTF(), in.readLong());
	}

	@Override
	public int getTypeId()
	{
		return PersistDocumentMessage.ID;
	}

	@Override
	public void destroy()
	{
	}
}
