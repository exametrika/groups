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
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;

/**
 * The {@link MessageRouter} represents a router of messages in protocol stack based on their content.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageRouter extends AbstractCompositeProtocol
{
    private final List<Pair<ICondition<IMessage>, IReceiver>> routes;
    
    public MessageRouter(String channelName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols, 
        List<Pair<ICondition<IMessage>, IReceiver>> routes)
    {
        this(channelName, null, messageFactory, protocols, routes);
    }
    
    public MessageRouter(String channelName, String loggerName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols, 
        List<Pair<ICondition<IMessage>, IReceiver>> routes)
    {
        super(channelName, loggerName, messageFactory, protocols);
        
        Assert.notNull(routes);
        
        this.routes = routes;
    }
    
    @Override
    protected void doWire(List<AbstractProtocol> protocols)
    {
        for (AbstractProtocol protocol : protocols)
        {
            protocol.setTimeService(timeService);
            protocol.setSender(getSender());
            protocol.setPullableSender(getPullableSender());
            protocol.setReceiver(getReceiver());
        }
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        for (Pair<ICondition<IMessage>, IReceiver> pair : routes)
        {
            if (pair.getKey().evaluate(message))
            {
                pair.getValue().receive(message);
                return;
            }
        }
        
        receiver.receive(message);
    }
}
