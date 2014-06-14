package serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import model.DocumentMetadata;

import java.io.IOException;

public class DocumentMetadataStreamSerializer implements StreamSerializer<DocumentMetadata>
{
	@Override
	public void write(final ObjectDataOutput out, final DocumentMetadata object) throws IOException
	{
		out.writeUTF((String) object.getPartitionKey());
		out.writeUTF(object.getKey());
		out.writeLong(object.getCreated());
		out.writeBoolean(object.isRead());
	}

	@Override
	public DocumentMetadata read(final ObjectDataInput in) throws IOException
	{
		return new DocumentMetadata(in.readUTF(), in.readUTF(), in.readLong(), in.readBoolean());
	}

	@Override
	public int getTypeId()
	{
		return DocumentMetadata.ID;
	}

	@Override
	public void destroy()
	{
	}
}
