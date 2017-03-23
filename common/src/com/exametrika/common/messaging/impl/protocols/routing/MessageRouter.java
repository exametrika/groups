/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.routing;

import java.util.List;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.AbstractCompositeProtocol;

/**
 * The {@link MessageRouter} represents a router of messages in protocol stack based on their content.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class MessageRouter extends AbstractCompositeProtocol
{
    public MessageRouter(String channelName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        this(channelName, null, messageFactory, protocols);
    }
    
    public MessageRouter(String channelName, String loggerName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        super(channelName, loggerName, messageFactory, protocols);
    }
    
    @Override
    protected void doWire(List<AbstractProtocol> protocols)
    {
        for (AbstractProtocol protocol : protocols)
            doWire(protocol);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (doRoute(message))
            return;
        else
            receiver.receive(message);
    }
    
    @Override
    protected void doWire(AbstractProtocol protocol)
    {
        protocol.setTimeService(timeService);
        protocol.setConnectionProvider(connectionProvider);
        protocol.setSender(getSender());
        protocol.setPullableSender(getPullableSender());
        protocol.setReceiver(getReceiver());
    }
    
    protected abstract boolean doRoute(IMessage message);
}
