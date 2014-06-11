package storage;

import stats.CustomStatisticValue.ValueCollector;

class StorageQueueSizeCollector implements ValueCollector<Long>
{
	private final StorageController storageController;

	public StorageQueueSizeCollector(final StorageController storageController)
	{
		this.storageController = storageController;
	}

	@Override
	public Long collectValue()
	{
		return storageController.getQueueSize();
	}

	@Override
	public String getType()
	{
		return "java.lang.Long";
	}
}
