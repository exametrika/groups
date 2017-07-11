/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupsMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.INodesMembership;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;

/**
 * The {@link OutGroupFailureAtomicMulticastSendProtocol} represents an out-group failure atomic reliable multicast send protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class OutGroupFailureAtomicMulticastSendProtocol extends AbstractFailureAtomicMulticastSendProtocol implements IClusterMembershipListener
{
    private final IClusterMembershipService membershipService;
    private IGroup group;
    private INodesMembership nodesMembership;
    
    public OutGroupFailureAtomicMulticastSendProtocol(String channelName, IMessageFactory messageFactory, 
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, 
        long maxUnacknowledgedPeriod, IDeliveryHandler senderDeliveryHandler, boolean durable, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity, ISerializationRegistry serializationRegistry,
        IClusterMembershipService membershipService, GroupAddress groupAddress, UUID groupId)
    {
        super(channelName, messageFactory, maxBundlingMessageSize, maxBundlingPeriod, maxBundleSize, maxUnacknowledgedPeriod, 
            senderDeliveryHandler, durable, maxUnlockQueueCapacity, minLockQueueCapacity, serializationRegistry, groupAddress, groupId);
       
        Assert.notNull(membershipService);
        
        this.membershipService = membershipService;
        this.sendQueue = new OutGroupSendQueue(this, this, senderDeliveryHandler, durable, maxUnlockQueueCapacity, 
            minLockQueueCapacity, messageFactory, groupAddress, groupId, logger, marker);
    }

    @Override
    public void onJoined()
    {
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        String domain = membershipService.getLocalNode().getDomain();
        IDomainMembership domainMembership = event.getNewMembership().findDomain(domain);
        nodesMembership = domainMembership.findElement(INodesMembership.class);
        IGroupsMembership groupsMembership = domainMembership.findElement(IGroupsMembership.class);
        group = groupsMembership.findGroup(groupId);
    }

    @Override
    protected long acquireTotalOrder(IMessage message)
    {
        return 0;
    }

    @Override
    protected IMessage acknowledgePiggyback(IMessage message)
    {
        return message;
    }

    @Override
    protected boolean isFlush()
    {
        return false;
    }

    @Override
    protected IGroup getGroup(boolean onStartFlush)
    {
        return group;
    }

    @Override
    protected boolean isFailedOrLeftNode(INode node)
    {
        return !isHealthyNode(node);
    }

    @Override
    protected boolean isHealthyNode(INode node)
    {
        if (nodesMembership == null)
            return false;
        
        return nodesMembership.findNode(node.getId()) != null;
    }
}
