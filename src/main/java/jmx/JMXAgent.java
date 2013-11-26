package jmx;

import java.lang.management.ManagementFactory;

import stats.StatisticManager;

import javax.management.MBeanServer;

public class JMXAgent
{
	private StatisticManager statisticManager;
	
	protected void init()
	{
		try
		{
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

			// Provide StatisticManager with data for JMX agent 
			statisticManager.registerMBean(mbs, "de.donnerbart:type=StatisticManager");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
