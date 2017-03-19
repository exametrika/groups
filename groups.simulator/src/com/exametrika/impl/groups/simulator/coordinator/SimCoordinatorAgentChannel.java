/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.io.File;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;



/**
 * The {@link SimCoordinatorAgentChannel} represents a simulator coordinator to agent channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinatorAgentChannel
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(SimCoordinatorAgentChannel.class);
    private final IAddress address;
    private final IMarker marker;
    private final IChannel channel;
    private boolean closed;
    
    public SimCoordinatorAgentChannel(IChannel channel, IAddress node, IMarker parentMarker)
    {
        Assert.notNull(channel);
        Assert.notNull(node);
        
        this.channel = channel;
        this.address = node;
        
        marker = Loggers.getMarker("connection:" + node, parentMarker);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());
    }
    
    public IAddress getNode()
    {
        return address;
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public void close()
    {
        synchronized (this)
        {
            if (closed)
                return;
            
            closed = true;
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    public void send(IMessagePart part)
    {
        IMessage message = channel.getMessageFactory().create(address, part);
        channel.send(message);    
    }
    
    public void send(IMessagePart part, List<File> files)
    {
        IMessage message = channel.getMessageFactory().create(address, part, 0, files);
        channel.send(message);    
    }
    
    private interface IMessages
    {
        @DefaultMessage("Simulator coordinator to agent channel is started.")
        ILocalizedMessage started();
        
        @DefaultMessage("Simulator coordinator to agent channel is stopped.")
        ILocalizedMessage stopped();
    }
}
