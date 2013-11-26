package controller;

import stats.CustomStatisticValue.ValueCollector;

class QueueSizeCollector implements ValueCollector<Integer>
{
	private StorageController storageController;

	public QueueSizeCollector(final StorageController storageController)
	{
		this.storageController = storageController;
	}

	@Override
	public Integer collectValue()
	{
		return storageController.size();
	}

	@Override
	public String getType()
	{
		return "java.lang.Integer";
	}
}
