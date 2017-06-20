package com.exametrika.tests.groups.load;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupParameters;

public class TestGroupChannelReconnector extends TestChannelReconnector
{
    private final TestGroupChannelFactory channelFactory;
    private final TestGroupParameters parameters;
    
    public TestGroupChannelReconnector(long reconnectPeriod, TestGroupChannelFactory channelFactory, TestGroupParameters parameters)
    {
        super(reconnectPeriod);
        
        Assert.notNull(channelFactory);
        Assert.notNull(parameters);
        
        this.channelFactory = channelFactory;
        this.parameters = parameters;
    }
    
    @Override
    public void run()
    {
        IChannel channel = channelFactory.createChannel(parameters);
        channel.start();
    }
}
