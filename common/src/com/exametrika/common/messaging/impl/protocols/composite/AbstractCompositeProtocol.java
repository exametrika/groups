/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.composite;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;

/**
 * The {@link AbstractCompositeProtocol} represents an abstract composite protocol.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractCompositeProtocol extends AbstractProtocol
{
    protected final List<AbstractProtocol> protocols;
    protected ISerializationRegistry registry;
    protected boolean started;
    protected boolean stopped;
    
    public AbstractCompositeProtocol(String channelName, IMessageFactory messageFactory, List<? extends AbstractProtocol> protocols)
    {
        this(channelName, null, messageFactory, protocols);
    }
    
    public AbstractCompositeProtocol(String channelName, String loggerName, IMessageFactory messageFactory, 
        List<? extends AbstractProtocol> protocols)
    {
        super(channelName, loggerName, messageFactory);
        
        Assert.notNull(protocols);
        Assert.isTrue(!protocols.isEmpty());
        
        this.protocols = new ArrayList<AbstractProtocol>(protocols);
    }
    
    public AbstractProtocol getFirst()
    {
        return protocols.get(0);
    }
    
    public AbstractProtocol getLast()
    {
        return protocols.get(protocols.size() - 1);
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
    public void register(ISerializationRegistry registry)
    {
        Assert.notNull(registry);
        
        this.registry = registry;
        
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

    /**
     * Performs wiring of dependencies in specified protocols.
     *
     * @param protocols protocols to wire dependencies in
     */
    protected abstract void doWire(List<AbstractProtocol> protocols);
    
    /**
     * Performs wiring of dependencies in specified protocol.
     *
     * @param protocol protocol to wire dependencies in
     */
    protected void doWire(AbstractProtocol protocol)
    {
    }
    
    protected final void addProtocol(AbstractProtocol protocol)
    {
        Assert.notNull(protocol);
        Assert.checkState(started && !stopped);
        
        protocol.setTimeService(timeService);
        protocol.setConnectionProvider(connectionProvider);
        doWire(protocol);
        registry.register(protocol);
        
        protocol.start();
    }
    
    protected final void removeProtocol(AbstractProtocol protocol)
    {
        Assert.notNull(protocol);
        Assert.checkState(started && !stopped);
        
        protocol.stop();
        registry.unregister(protocol);
    }
}
