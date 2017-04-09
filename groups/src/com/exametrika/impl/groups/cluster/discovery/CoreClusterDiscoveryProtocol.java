/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
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
public final class CoreClusterDiscoveryProtocol extends AbstractProtocol implements IWorkerNodeDiscoverer
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipService membershipService;
    private final IGroupFailureDetector failureDetector;
    private Set<INode> discoveredNodes = new HashSet<INode>();

    public CoreClusterDiscoveryProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipService membershipService, 
        IGroupFailureDetector failureDetector)
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
            {
                discoveredNodes.addAll(part.getDiscoveredNodes());
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.nodesDiscovered(discoveredNodes));
            }
            else
                send(messageFactory.create(currentCoordinator.getAddress(), part, MessageFlags.HIGH_PRIORITY));
        }
        else
            receiver.receive(message);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Nodes ''{0}'' have been discovered.")
        ILocalizedMessage nodesDiscovered(Set<INode> nodes);
    }
}
