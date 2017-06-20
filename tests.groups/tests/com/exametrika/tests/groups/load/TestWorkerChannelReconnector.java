package com.exametrika.tests.groups.load;

import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.utils.Assert;

public class TestWorkerChannelReconnector extends TestChannelReconnector
{
    private final TestWorkerNodeChannelFactory channelFactory;
    private final TestWorkerNodeParameters parameters;
    
    public TestWorkerChannelReconnector(long reconnectPeriod, TestWorkerNodeChannelFactory channelFactory, TestWorkerNodeParameters parameters)
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
