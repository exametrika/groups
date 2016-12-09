/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.error;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;

/**
 * The {@link UnhandledMessageProtocol} represents a protocol logs all unhandled messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UnhandledMessageProtocol extends AbstractProtocol
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    public UnhandledMessageProtocol(String channelName, IMessageFactory messageFactory)
    {
        this(channelName, null, messageFactory);
    }
    
    public UnhandledMessageProtocol(String channelName, String loggerName, IMessageFactory messageFactory)
    {
        super(channelName, loggerName, messageFactory);
    }
    
    @Override
    public void start()
    {
        blankOffReceiver();
        super.start();
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
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
