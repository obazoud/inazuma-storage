package jmx;

import stats.StatisticManager;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class JMXAgent
{
	private static final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	public JMXAgent()
	{
		try
		{
			mbs.registerMBean(new InazumaStorageWrapper(), new ObjectName("de.donnerbart:type=InazumaStorage"));

			// Provide StatisticManager with data for JMX agent
			StatisticManager.getInstance().registerMBean(mbs, "de.donnerbart:type=StatisticManager");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
