package com.exametrika.tests.groups.load;

import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.utils.Assert;

public class TestCoreChannelReconnector extends TestChannelReconnector
{
    private final TestCoreNodeChannelFactory channelFactory;
    private final TestCoreNodeParameters parameters;
    
    public TestCoreChannelReconnector(long reconnectPeriod, TestCoreNodeChannelFactory channelFactory, TestCoreNodeParameters parameters)
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
        ICompositeChannel channel = channelFactory.createChannel(parameters);
        channel.start();
    }
}
