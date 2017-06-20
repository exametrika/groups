package com.exametrika.tests.groups.load;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

public abstract class TestChannelReconnector implements IChannelReconnector, Runnable
{
    private final long reconnectPeriod;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    
    public TestChannelReconnector(long reconnectPeriod)
    {
        this.reconnectPeriod = reconnectPeriod;
    }
    
    @Override
    public void reconnect()
    {
        executor.schedule(this, reconnectPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public abstract void run();
}
