/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.NodeMembership;
import com.exametrika.impl.groups.core.failuredetection.FailureUpdateMessagePart;
import com.exametrika.impl.groups.core.failuredetection.FailureUpdateMessagePartSerializer;

/**
 * The {@link CoreCoordinatorFailureDetectionProtocol} represents a core coordinator part of cluster failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreCoordinatorFailureDetectionProtocol extends AbstractProtocol implements IWorkerFailureDetector
{
    private final IClusterMembershipService membershipService;
    private Set<INode> failedMembers = new LinkedHashSet<INode>();
    private Set<INode> leftMembers = new LinkedHashSet<INode>();

    public CoreCoordinatorFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipService membershipService)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        
        this.membershipService = membershipService;
    }

    @Override
    public Set<INode> takeFailedNodes()
    {
        Set<INode> failedMembers = this.failedMembers;
        this.failedMembers = new LinkedHashSet<INode>();
        return failedMembers;
    }

    @Override
    public Set<INode> takeLeftNodes()
    {
        Set<INode> leftMembers = this.leftMembers;
        this.leftMembers = new LinkedHashSet<INode>();
        return leftMembers;
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FailureUpdateMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FailureUpdateMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof FailureUpdateMessagePart)
        {
            FailureUpdateMessagePart part = message.getPart();
            IClusterMembership membership = membershipService.getMembership();
            if (membership == null)
                return;
            
            for (UUID nodeId : part.getFailedMembers())
            {
                INode node = findNode(membership, nodeId);
                if (node != null)
                    failedMembers.add(node);
            }
            
            for (UUID nodeId : part.getLeftMembers())
            {
                INode node = findNode(membership, nodeId);
                if (node != null)
                    leftMembers.add(node);
            }
        }
        else
            receiver.receive(message);
    }

    private INode findNode(IClusterMembership membership, UUID nodeId)
    {
        for (IDomainMembership domain : membership.getDomains())
        {
            NodeMembership nodeMembership = domain.findElement(NodeMembership.class);
            INode node = nodeMembership.findNode(nodeId);
            if (node != null)
                return node;
        }

        return null;
    }
}
