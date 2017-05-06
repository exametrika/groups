/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.composite;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
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
import com.exametrika.common.utils.Assert;

/**
 * The {@link MessageRouter} represents a router of messages in protocol stack based on their content.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class MessageRouter extends AbstractProtocol
{
    protected final List<AbstractProtocol> protocols = new ArrayList<AbstractProtocol>();
    private boolean started;
    private boolean stopped;
    private ISerializationRegistry registry;
    
    public MessageRouter(String channelName, IMessageFactory messageFactory)
    {
        this(channelName, null, messageFactory);
    }
    
    public MessageRouter(String channelName, String loggerName, IMessageFactory messageFactory)
    {
        super(channelName, loggerName, messageFactory);
    }
    
    @Override
    public void start()
    {
        super.start();
        
        started = true;
    }

    @Override
    public void stop()
    {
        stopped = true;
        
        for (AbstractProtocol protocol : protocols)
            protocol.stop();
        
        super.stop();
    }

    @Override
    public void onTimer(long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.onTimer(currentTime);
    }
    
    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.cleanup(cleanupManager, liveNodeProvider, currentTime);
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        Assert.notNull(registry);
        
        this.registry = registry;
    }
    
    protected void doWire(AbstractProtocol protocol)
    {
        protocol.setSender(getSender());
        protocol.setPullableSender(getPullableSender());
        protocol.setReceiver(getReceiver());
    }
    
    protected final void addProtocol(AbstractProtocol protocol)
    {
        Assert.notNull(protocol);
        Assert.checkState(started && !stopped);
        
        protocol.setTimeService(timeService);
        protocol.setConnectionProvider(connectionProvider);
        doWire(protocol);
        
        if (registry != null && protocols.isEmpty())
            protocol.register(registry);
        
        protocol.start();
        protocols.add(protocol);
    }
    
    protected final void removeProtocol(AbstractProtocol protocol)
    {
        Assert.notNull(protocol);
        Assert.checkState(started && !stopped);
        
        protocol.stop();
        protocols.remove(protocol);
        
        if (registry != null && protocols.isEmpty())
            protocol.unregister(registry);
    }
    
    @Override
    protected boolean supportsPullSendModel()
    {
        return false;
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (doReceiveRoute(message))
            return;
        else
            receiver.receive(message);
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (doSendRoute(message))
            return;
        else
            sender.send(message);
    }
    
    @Override
    protected ISink doRegister(IPullableSender pullableSender, IAddress destination, IFeed feed)
    {
        ISink sink = doRegisterRoute(destination, feed);
        if (sink != null)
            return sink;
        else
            return pullableSender.register(destination, feed);
    }
    
    @Override
    protected void doUnregister(IPullableSender pullableSender, ISink sink)
    {
        if (doUnregisterRoute(sink))
            return;
        else
            pullableSender.unregister(sink);
    }
    
    protected abstract boolean doReceiveRoute(IMessage message);
    protected abstract boolean doSendRoute(IMessage message);
    protected abstract ISink doRegisterRoute(IAddress destination, IFeed feed);
    protected abstract boolean doUnregisterRoute(ISink sink);
}
