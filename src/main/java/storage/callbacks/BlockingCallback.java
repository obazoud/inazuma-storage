package storage.callbacks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingCallback<T>
{
	private final AtomicReference<T> result = new AtomicReference<>(null);
	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	public T getResult()
	{
		lock.lock();
		try
		{
			condition.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException ignored)
		{
		}
		finally
		{
			lock.unlock();
		}
		return result.get();
	}

	public void setResult(final T result)
	{
		lock.lock();
		try
		{
			this.result.set(result);
			condition.signal();
		}
		finally
		{
			lock.unlock();
		}
	}
}
