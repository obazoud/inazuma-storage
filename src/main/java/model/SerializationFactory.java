package model;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class SerializationFactory implements DataSerializableFactory
{
	public static final int SERIALIZED_DATA = 1;

	@Override
	public IdentifiedDataSerializable create(int factoryID)
	{
		switch (factoryID)
		{
			case SERIALIZED_DATA:
				return new SerializedData();
		}
		return null;
	}
}
