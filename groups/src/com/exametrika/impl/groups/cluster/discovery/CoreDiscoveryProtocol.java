/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.discovery.DiscoveryMessagePart;
import com.exametrika.impl.groups.core.discovery.DiscoveryMessagePartSerializer;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;

/**
 * The {@link CoreDiscoveryProtocol} represents a core group part of discovery protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreDiscoveryProtocol extends AbstractProtocol implements IWorkerNodeDiscoverer
{
    private final IMembershipService membershipService;
    private final IFailureDetector failureDetector;
    private Set<INode> discoveredNodes = new HashSet<INode>();

    public CoreDiscoveryProtocol(String channelName, IMessageFactory messageFactory, IMembershipService membershipService, 
        IFailureDetector failureDetector)
    {
        super(channelName, messageFactory);

        Assert.notNull(membershipService);
        Assert.notNull(failureDetector);

        this.membershipService = membershipService;
        this.failureDetector = failureDetector;
    }

    @Override
    public Set<INode> takeDiscoveredNodes()
    {
        if (!discoveredNodes.isEmpty())
        {
            Set<INode> nodes = discoveredNodes;
            discoveredNodes = new HashSet<INode>();
            
            return nodes;
        }
        else
            return Collections.emptySet();
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
        if (message.getPart() instanceof DiscoveryMessagePart)
        {
            DiscoveryMessagePart part = message.getPart();

            INode currentCoordinator = failureDetector.getCurrentCoordinator();
            if (currentCoordinator == null)
                return;
            
            if (currentCoordinator.equals(membershipService.getLocalNode()))
                discoveredNodes.addAll(part.getDiscoveredNodes());
            else
                send(messageFactory.create(currentCoordinator.getAddress(), part, MessageFlags.HIGH_PRIORITY));
        }
        else
            receiver.receive(message);
    }
}
