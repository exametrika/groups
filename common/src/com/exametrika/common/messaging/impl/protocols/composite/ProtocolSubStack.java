/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.composite;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ProtocolSubStack} represents a a sub-stack of protocols.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ProtocolSubStack extends AbstractProtocol
{
    private final List<AbstractProtocol> protocols;
    
    public ProtocolSubStack(String channelName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        this(channelName, null, messageFactory, protocols);
    }
    
    public ProtocolSubStack(String channelName, String loggerName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        super(channelName, loggerName, messageFactory);
        
        Assert.notNull(protocols);
        Assert.isTrue(!protocols.isEmpty());
        
        this.protocols = new ArrayList<AbstractProtocol>(protocols);
    }
    
    public <T> T find(Class<T> protocolClass)
    {
        Assert.notNull(protocolClass);
        
        for (AbstractProtocol protocol : protocols)
        {
            if (protocolClass.isAssignableFrom(protocol.getClass()))
                return (T)protocol;
        }
        
        return null;
    }
    
    @Override
    public void setTimeService(ITimeService timeService)
    {
        super.setTimeService(timeService);
        
        for (AbstractProtocol protocol : protocols)
            protocol.setTimeService(timeService);
    }
    
    @Override
    public void setConnectionProvider(IConnectionProvider connectionProvider)
    {
        super.setConnectionProvider(connectionProvider);
        
        for (AbstractProtocol protocol : protocols)
            protocol.setConnectionProvider(connectionProvider);
    }
    
    @Override
    public void start()
    {
        super.start();
        
        doWire(protocols);
        
        for (AbstractProtocol protocol : protocols)
            protocol.start();
    }

    @Override
    public void stop()
    {
        for (AbstractProtocol protocol : protocols)
            protocol.stop();
        
        super.stop();
    }

    @Override
    public void onTimer(long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
        {
            if (protocol.isEnabled())
                protocol.onTimer(currentTime);
        }
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        Assert.notNull(registry);
        
        for (AbstractProtocol protocol : protocols)
            protocol.register(registry);
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.unregister(registry);
    }
    
    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.cleanup(cleanupManager, liveNodeProvider, currentTime);
    }

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
            next.setReceiver(prev);
        }
        
        AbstractProtocol last = protocols.get(protocols.size() - 1);
        last.setSender(getSender());
        last.setPullableSender(getPullableSender());
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
