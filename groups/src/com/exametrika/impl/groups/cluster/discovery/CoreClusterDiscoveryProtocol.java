/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;

/**
 * The {@link CoreClusterDiscoveryProtocol} represents a core group part of cluster discovery protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterDiscoveryProtocol extends AbstractProtocol
{
    private IGroupFailureDetector failureDetector;
    private ISender bridgeSender;

    public CoreClusterDiscoveryProtocol(String channelName, IMessageFactory messageFactory)
    {
        super(channelName, messageFactory);
    }

    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }

    public void setBridgeSender(ISender bridgeSender)
    {
        Assert.notNull(bridgeSender);
        Assert.isNull(this.bridgeSender);
        
        this.bridgeSender = bridgeSender;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new DiscoveryMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(DiscoveryMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof DiscoveryMessagePart && !((DiscoveryMessagePart)message.getPart()).isCore())
        {
            DiscoveryMessagePart part = message.getPart();

            INode currentCoordinator = failureDetector.getCurrentCoordinator();
            if (currentCoordinator == null)
                return;
            
            bridgeSender.send(messageFactory.create(currentCoordinator.getAddress(), part, MessageFlags.HIGH_PRIORITY));
        }
        else
            receiver.receive(message);
    }
}
