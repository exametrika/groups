/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.composite;

import java.util.List;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;

/**
 * The {@link ProtocolSubStack} represents a a sub-stack of protocols.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ProtocolSubStack extends AbstractCompositeProtocol
{
    public ProtocolSubStack(String channelName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        this(channelName, null, messageFactory, protocols);
    }
    
    public ProtocolSubStack(String channelName, String loggerName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        super(channelName, loggerName, messageFactory, protocols);
    }
    
    @Override
    protected void doWire(List<AbstractProtocol> protocols)
    {
        AbstractProtocol first = protocols.get(0);
        first.setReceiver(getReceiver());
        
        for (int i = 0; i < protocols.size() - 1; i++)
        {
            AbstractProtocol prev = protocols.get(i);
            AbstractProtocol next = protocols.get(i + 1);
            
            prev.setSender(next);
            prev.setPullableSender(next);
            prev.setTimeService(getTimeService());
            next.setReceiver(prev);
        }
        
        AbstractProtocol last = protocols.get(protocols.size() - 1);
        last.setSender(getSender());
        last.setPullableSender(getPullableSender());
        last.setTimeService(getTimeService());
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        ISender first = protocols.get(0);
        first.send(message);
    }
    
    @Override
    protected ISink doRegister(IPullableSender pullableSender, IAddress destination, IFeed feed)
    {
        IPullableSender first = protocols.get(0);
        return first.register(destination, feed);
    }

    @Override
    protected void doUnregister(IPullableSender pullableSender, ISink sink)
    {
        IPullableSender first = protocols.get(0);
        first.unregister(sink);
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        IReceiver last = protocols.get(protocols.size() - 1);
        last.receive(message);
    }
}
