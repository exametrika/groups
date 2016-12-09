/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.composite;

import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
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
        
        this.protocols = (List)protocols;
    }
    
    @Override
    public final void start()
    {
        super.start();
        
        doWire(protocols);
        
        for (AbstractProtocol protocol : protocols)
            protocol.start();
    }

    @Override
    public final void stop()
    {
        for (AbstractProtocol protocol : protocols)
            protocol.stop();
        
        super.stop();
    }

    @Override
    public final void onTimer(long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.onTimer(currentTime);
    }
    
    @Override
    public final void register(ISerializationRegistry registry)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.register(registry);
    }

    @Override
    public final void unregister(ISerializationRegistry registry)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.unregister(registry);
    }
    
    @Override
    public final void cleanup(ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.cleanup(liveNodeProvider, currentTime);
    }

    /**
     * Performs wiring of dependencies in specified protocols.
     *
     * @param protocols protocols to wire dependencies in
     */
    protected abstract void doWire(List<AbstractProtocol> protocols);
}
