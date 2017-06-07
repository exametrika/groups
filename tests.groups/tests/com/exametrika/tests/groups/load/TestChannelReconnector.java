package com.exametrika.tests.groups.load;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupParameters;

public class TestChannelReconnector implements IChannelReconnector, Runnable
{
    private final long reconnectPeriod;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final TestGroupChannelFactory channelFactory;
    private final TestGroupParameters parameters;
    
    public TestChannelReconnector(long reconnectPeriod, TestGroupChannelFactory channelFactory, TestGroupParameters parameters)
    {
        Assert.notNull(channelFactory);
        Assert.notNull(parameters);
        
        this.reconnectPeriod = reconnectPeriod;
        this.channelFactory = channelFactory;
        this.parameters = parameters;
    }
    
    @Override
    public void reconnect()
    {
        executor.schedule(this, reconnectPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run()
    {
        IChannel channel = channelFactory.createChannel(parameters);
        channel.start();
    }
}
