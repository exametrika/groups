/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols;

import java.util.List;

import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.protocols.failuredetection.CleanupManager;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link ProtocolStack} represents a stack of protocols.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ProtocolStack implements ILifecycle, ICompartmentProcessor, ISerializationRegistrar
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(ProtocolStack.class);
    private final IMarker marker;
    private final List<AbstractProtocol> protocols;
    private final CleanupManager cleanupManager;
    private ITimeService timeService;

    public ProtocolStack(String channelName, List<AbstractProtocol> protocols, ILiveNodeProvider liveNodeProvider,
        long cleanupPeriod, long nodeCleanupPeriod)
    {
        Assert.notNull(channelName);
        Assert.notNull(protocols);
        Assert.isTrue(!protocols.isEmpty());
        Assert.notNull(liveNodeProvider);
        
        marker = Loggers.getMarker(channelName);
        this.protocols = protocols;
        this.cleanupManager = new CleanupManager(protocols, liveNodeProvider, cleanupPeriod, nodeCleanupPeriod);
    }
    
    public AbstractProtocol getFirst()
    {
        return protocols.get(0);
    }
    
    public AbstractProtocol getLast()
    {
        return protocols.get(protocols.size() - 1);
    }
    
    public <T extends AbstractProtocol> T find(Class<T> protocolClass)
    {
        for (AbstractProtocol protocol : protocols)
        {
            if (protocol.getClass() == protocolClass)
                return (T)protocol;
        }
        return null;
    }
    
    public void setTimeService(ITimeService timeService)
    {
        Assert.notNull(timeService);
        Assert.isNull(this.timeService);
        
        this.timeService = timeService;
        
        for (AbstractProtocol protocol : protocols)
            protocol.setTimeService(timeService);
        
        cleanupManager.setTimeService(timeService);
    }
    
    public void setConnectionProvider(IConnectionProvider connectionProvider)
    {
        Assert.notNull(connectionProvider);
        
        for (AbstractProtocol protocol : protocols)
            protocol.setConnectionProvider(connectionProvider);
    }
    
    @Override
    public void start()
    {
        Assert.notNull(timeService);
        
        for (int i = 0; i < protocols.size() - 1; i++)
        {
            AbstractProtocol prev = protocols.get(i);
            AbstractProtocol next = protocols.get(i + 1);
            
            prev.setSender(next);
            prev.setPullableSender(next);
            next.setReceiver(prev);
        }
            
        for (AbstractProtocol protocol : protocols)
            protocol.start();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.protocolStackStarted());
    }

    @Override
    public void stop()
    {
        for (AbstractProtocol protocol : protocols)
            protocol.stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.protocolStackStopped());
    }

    @Override
    public void onTimer(long currentTime)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.onTimer(currentTime);
        
        cleanupManager.onTimer(currentTime);
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.register(registry);
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        for (AbstractProtocol protocol : protocols)
            protocol.unregister(registry);
    }

    private interface IMessages
    {
        @DefaultMessage("Protocol stack has been started.")
        ILocalizedMessage protocolStackStarted();
        @DefaultMessage("Protocol stack has been stopped.")
        ILocalizedMessage protocolStackStopped();
    }
}
