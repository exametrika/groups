/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;

/**
 * The {@link WorkerGroupDiscoveryProtocol} represents a protocol that discovers worker group.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupDiscoveryProtocol extends AbstractProtocol implements IGroupNodeDiscoverer, 
    IClusterMembershipListener
{
    private final UUID groupId;
    private final IGroupMembershipService groupMembershipService;
    private final IClusterMembershipService clusterMembershipService;
    private final IGroupFailureDetector failureDetector;
    private final Listener listener = new Listener();
    private IGroupJoinStrategy groupJoinStrategy;
    private IGroupMembership membership;
    private INode coordinator;
    
    public WorkerGroupDiscoveryProtocol(String channelName, IMessageFactory messageFactory, UUID groupId,
        IGroupMembershipService groupMembershipService, IClusterMembershipService clusterMembershipService,
        IGroupFailureDetector failureDetector)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(groupId);
        Assert.notNull(groupMembershipService);
        Assert.notNull(clusterMembershipService);
        Assert.notNull(failureDetector);
        
        this.groupId = groupId;
        this.groupMembershipService = groupMembershipService;
        this.clusterMembershipService = clusterMembershipService;
        this.failureDetector = failureDetector;
    }
    
    public void setGroupJoinStrategy(IGroupJoinStrategy groupJoinStrategy)
    {
        Assert.notNull(groupJoinStrategy);
        Assert.isNull(this.groupJoinStrategy);
        
        this.groupJoinStrategy = groupJoinStrategy;
    }

    public IGroupMembershipListener getGroupMembershipListener()
    {
        return listener;
    }
    
    @Override
    public Set<INode> getDiscoveredNodes()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void onJoined()
    {
        setClusterMembership(clusterMembershipService.getMembership());
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        setClusterMembership(event.getNewMembership());
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.hasFlags(MessageFlags.GROUP_DISCOVERY_REQUEST))
        {
            if (membership == null)
                return;
            if (!membership.getGroup().getCoordinator().equals(groupMembershipService.getLocalNode()))
                return;
            
            send(messageFactory.create(message.getSource(), new MembershipResponseMessagePart(membership.getId(), 
                getNodeAddresses(failureDetector.getHealthyMembers()))));
        }
        else if (message.getPart() instanceof MembershipResponseMessagePart)
        {
            MembershipResponseMessagePart part = message.getPart();
            if (membership != null)
                return;
            
            groupJoinStrategy.onGroupDiscovered(part.getHealthyMembers());
        }
        else
            receiver.receive(message);
    }
    
    private void setGroupMembership(IGroupMembership membership)
    {
        this.membership = membership;
    }
    
    private void setClusterMembership(IClusterMembership membership)
    {
        if (this.membership != null)
            return;
        
        IDomainMembership domainMembership = membership.findDomain(clusterMembershipService.getLocalNode().getDomain());
        if (domainMembership == null)
            return;
        GroupsMembership groupsMembershipp = domainMembership.findElement(GroupsMembership.class);
        IGroup group = groupsMembershipp.findGroup(groupId);
        if (group == null)
            return;
        
        if (group.getCoordinator().equals(clusterMembershipService.getLocalNode()))
            return;
        
        if (coordinator != null && coordinator.equals(group.getCoordinator()))
            return;
        
        this.coordinator = group.getCoordinator();
        send(messageFactory.create(coordinator.getAddress(), MessageFlags.GROUP_DISCOVERY_REQUEST));
    }
    
    private class Listener implements IGroupMembershipListener
    {
        @Override
        public void onJoined()
        {
            setGroupMembership(groupMembershipService.getMembership());
        }

        @Override
        public void onMembershipChanged(GroupMembershipEvent event)
        {
            setGroupMembership(event.getNewMembership());
        }
        
        @Override
        public void onLeft(LeaveReason reason)
        {
        }
    }
}
