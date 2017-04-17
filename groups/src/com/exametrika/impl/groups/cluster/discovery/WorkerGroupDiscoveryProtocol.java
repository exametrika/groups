/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;

/**
 * The {@link WorkerGroupDiscoveryProtocol} represents a protocol that discovers worker group.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupDiscoveryProtocol extends AbstractProtocol implements IGroupNodeDiscoverer, 
    IGroupMembershipListener, IPreparedGroupMembershipListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID groupId;
    private final IGroupMembershipService groupMembershipService;
    private final IClusterMembershipService clusterMembershipService;
    private final IGroupFailureDetector failureDetector;
    private final Listener listener = new Listener();
    private IGroupJoinStrategy groupJoinStrategy;
    private IGroupMembership membership;
    private INode coordinator;
    private TreeSet<INode> discoveredNodes = new TreeSet<INode>();
    
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

    public IClusterMembershipListener getGroupMembershipListener()
    {
        return listener;
    }
    
    @Override
    public Set<INode> getDiscoveredNodes()
    {
        return discoveredNodes;
    }
    
    @Override
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
    {
        discoveredNodes.removeAll(newMembership.getGroup().getMembers());
    }
    
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
            if (membership != null)
                return;
            
            MembershipResponseMessagePart part = message.getPart();
            groupJoinStrategy.onGroupDiscovered(part.getHealthyMembers());
        }
        else if (message.getPart() instanceof GroupJoinMessagePart)
        {
            if (membership == null)
                return;
            
            GroupJoinMessagePart part = message.getPart();
            INode joiningNode = part.getJoiningNode();
            if (groupMembershipService.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
            {
                if (!discoveredNodes.contains(joiningNode) && membership.getGroup().findMember(joiningNode.getId()) == null)
                {
                    discoveredNodes.add(joiningNode);
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(joiningNode));
                }
            }
            else
                send(messageFactory.create(failureDetector.getCurrentCoordinator().getAddress(), part));
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
        
        GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
        IGroup group = groupsMembership.findGroup(groupId);
        if (group == null)
            return;
        
        if (group.getCoordinator().equals(clusterMembershipService.getLocalNode()))
            return;
        
        if (coordinator != null && coordinator.equals(group.getCoordinator()))
            return;
        
        this.coordinator = group.getCoordinator();
        send(messageFactory.create(coordinator.getAddress(), MessageFlags.GROUP_DISCOVERY_REQUEST));
    }
    
    private List<IAddress> getNodeAddresses(List<INode> nodes)
    {
        List<IAddress> addresses = new ArrayList<IAddress>(nodes.size());
        for (INode node : nodes)
            addresses.add(node.getAddress());
            
        return addresses;
    }
    
    private class Listener implements IClusterMembershipListener
    {
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
    }
    
    private interface IMessages
    {
        @DefaultMessage("Node ''{0}'' has been discovered.")
        ILocalizedMessage nodeDiscovered(INode node);
    }
}
