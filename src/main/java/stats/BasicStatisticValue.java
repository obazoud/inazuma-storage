package stats;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class BasicStatisticValue extends AbstractStatisticValue<Long>
{
	private final AtomicLong timeRangeValue = new AtomicLong(0);

	public BasicStatisticValue(final String group, final String name, final long duration, final TimeUnit timeUnit, final boolean autoRegister)
	{
		super(group, name, duration, timeUnit);
		lastTimeRangedValueDefault = 0L;
		stats.add(Stat.SUM);
		if (autoRegister)
		{
			StatisticManager.getInstance().registerStatisticValue(this);
		}
	}

	public BasicStatisticValue(final String group, final String name, final long duration, final TimeUnit timeUnit)
	{
		this(group, name, duration, timeUnit, true);
	}

	public BasicStatisticValue(final String name, final long duration, final TimeUnit timeUnit)
	{
		this(null, name, duration, timeUnit, true);
	}

	public BasicStatisticValue(final String group, final String name)
	{
		this(group, name, DEFAULT_INTERVAL, DEFAULT_UNIT, true);
	}

	public BasicStatisticValue(final String name)
	{
		this(null, name);
	}

	public void increment()
	{
		timeRangeValue.incrementAndGet();
	}

	public void increment(final long value)
	{
		timeRangeValue.addAndGet(value);
	}

	@Override
	protected String getType()
	{
		return "java.lang.Long";
	}

	@Override
	protected void swapValue()
	{
		lastTimeRangedValueDefault = timeRangeValue.getAndSet(0);
	}
}
