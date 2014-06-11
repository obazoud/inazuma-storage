package stats;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public abstract class AbstractStatisticValue<VALUE>
{
	public enum Stat
	{
		SUM, COUNT, AVG, MIN, MAX
	}

	static final long DEFAULT_INTERVAL = 10;
	static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

	final ValueValidator valueValidator = new ValueValidator();
	final EnumSet<Stat> stats = EnumSet.noneOf(Stat.class);

	volatile VALUE lastTimeRangedValueDefault = null;
	volatile VALUE lastTimeRangedValueCount = null;
	volatile VALUE lastTimeRangedValueAvg = null;
	volatile VALUE lastTimeRangedValueMin = null;
	volatile VALUE lastTimeRangedValueMax = null;

	private final String group;
	private final String name;
	private final long timeRange;

	private long lastTimeRangeTick = System.nanoTime();

	AbstractStatisticValue(final String group, final String name, final long duration, final TimeUnit timeUnit) throws IllegalArgumentException
	{
		if (name.endsWith("Cnt") || name.endsWith("Avg") || name.endsWith("Min") || name.endsWith("Max"))
		{
			throw new IllegalArgumentException("Parameter \"name\" is not allowed to end with \"Cnt\", \"Avg\", \"Min\" or \"Max\"!");
		}
		long timeRange = timeUnit.toNanos(duration);
		if (timeRange < TimeUnit.MILLISECONDS.toNanos(10))
		{
			throw new IllegalArgumentException("Parameter \"duration\" with \"timeUnit\" must be greater than 10 milliseconds!");
		}
		this.group = group;
		this.name = name;
		this.timeRange = timeRange;
	}

	public AbstractStatisticValue(final String name, final long duration, final TimeUnit timeUnit)
	{
		this(null, name, duration, timeUnit);
	}

	public AbstractStatisticValue(final String group, final String name, final long duration)
	{
		this(group, name, duration, DEFAULT_UNIT);
	}

	public AbstractStatisticValue(final String name)
	{
		this(null, name, DEFAULT_INTERVAL, DEFAULT_UNIT);
	}

	public boolean showSum()
	{
		return stats.contains(Stat.SUM);
	}

	public boolean showCount()
	{
		return stats.contains(Stat.COUNT);
	}

	public boolean showAvg()
	{
		return stats.contains(Stat.AVG);
	}

	public boolean showMin()
	{
		return stats.contains(Stat.MIN);
	}

	public boolean showMax()
	{
		return stats.contains(Stat.MAX);
	}

	public boolean isMemberOf(final String group)
	{
		return (this.group != null && this.group.equals(group));
	}

	public String getGroup()
	{
		return group;
	}

	public String getName()
	{
		return name;
	}

	public long getTimeRange()
	{
		return timeRange;
	}

	public VALUE getLastTimeRangedValueSum()
	{
		return lastTimeRangedValueDefault;
	}

	public VALUE getLastTimeRangedValueCount()
	{
		return lastTimeRangedValueCount;
	}

	public VALUE getLastTimeRangedValueAvg()
	{
		return lastTimeRangedValueAvg;
	}

	public VALUE getLastTimeRangedValueMin()
	{
		return lastTimeRangedValueMin;
	}

	public VALUE getLastTimeRangedValueMax()
	{
		return lastTimeRangedValueMax;
	}

	protected abstract String getType();

	protected abstract void swapValue();

	class ValueValidator implements Runnable
	{
		@Override
		public void run()
		{
			final long currentTime = System.nanoTime();
			if (currentTime - lastTimeRangeTick > timeRange)
			{
				lastTimeRangeTick = currentTime;
				swapValue();
			}
		}
	}
}
