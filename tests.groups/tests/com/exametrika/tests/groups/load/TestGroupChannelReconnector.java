package com.exametrika.tests.groups.load;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupParameters;

public class TestGroupChannelReconnector extends TestChannelReconnector
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestGroupChannelReconnector.class);
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
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.channelReconnected(channel));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Channel has been reconnected: {0}.")
        ILocalizedMessage channelReconnected(IChannel channel);
    }
}
