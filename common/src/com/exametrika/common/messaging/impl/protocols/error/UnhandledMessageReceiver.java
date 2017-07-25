/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.error;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;

/**
 * The {@link UnhandledMessageReceiver} represents a receiver logs all unhandled messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UnhandledMessageReceiver implements IReceiver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger;
    private final IMarker marker;
    
    public UnhandledMessageReceiver(String channelName)
    {
        Assert.notNull(channelName);
        
        logger = Loggers.get(getClass().getName());
        marker = Loggers.getMarker(channelName);
    }
    
    @Override
    public void receive(IMessage message)
    {
        if (logger.isLogEnabled(LogLevel.ERROR))
            logger.log(LogLevel.ERROR, marker, messages.unhandledMessageReceived(message));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Unhandled message has been received:\n{0}")
        ILocalizedMessage unhandledMessageReceived(IMessage message);
    }
}
