/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolSubStack;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.IGroupNodeDiscoverer;
import com.exametrika.impl.groups.cluster.feedback.DataLossState;
import com.exametrika.impl.groups.cluster.feedback.IDataLossFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.IDataLossState;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;

/**
 * The {@link GroupProtocolSubStack} represents a group protocol sub-stack.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class GroupProtocolSubStack extends ProtocolSubStack implements IPreparedGroupMembershipListener, 
    IClusterMembershipListener, ICompartmentProcessor
{
    private final UUID groupId;
    private final GroupMembershipManager membershipManager;
    private final IDataLossFeedbackService dataLossFeedbackService;
    private final int maxGroupMembershipHistorySize;
    private final List<IClusterMembershipListener> clusterMembershipListeners;
    private final List<ICompartmentProcessor> compartmentProcessors;
    private long startRemoveTime;
    private final Deque<IGroupMembership> membershipHistory = new ArrayDeque<IGroupMembership>();
    private IFlushManager flushManager;
    private IGroupNodeDiscoverer nodeDiscoverer;
    private Deque<MembershipInfo> pendingMemberships = new ArrayDeque<MembershipInfo>();
    private long lastUpdateTime;

    public GroupProtocolSubStack(String channelName, IMessageFactory messageFactory, UUID groupId,
        List<? extends AbstractProtocol> protocols, GroupMembershipManager membershipManager, 
        IDataLossFeedbackService dataLossFeedbackService, int maxGroupMembershipHistorySize,
        List<IClusterMembershipListener> clusterMembershipListeners, List<ICompartmentProcessor> compartmentProcessors)
    {
        super(channelName, messageFactory, protocols);
        
        Assert.notNull(groupId);
        Assert.notNull(membershipManager);
        Assert.notNull(dataLossFeedbackService);
        Assert.notNull(clusterMembershipListeners);
        Assert.notNull(compartmentProcessors);
        
        this.groupId = groupId;
        this.membershipManager = membershipManager;
        this.maxGroupMembershipHistorySize = maxGroupMembershipHistorySize;
        this.dataLossFeedbackService = dataLossFeedbackService;
        this.clusterMembershipListeners = clusterMembershipListeners;
        this.compartmentProcessors = compartmentProcessors;
    }

    public long getStartRemoveTime()
    {
        return startRemoveTime;
    }

    public void setStartRemoveTime(long startRemoveTime)
    {
        this.startRemoveTime = startRemoveTime;
    }

    @Override
    public void start()
    {
        super.start();
        
        membershipManager.start();
        flushManager = find(IFlushManager.class);
        Assert.notNull(flushManager);
        
        nodeDiscoverer = find(IGroupNodeDiscoverer.class);
        Assert.notNull(nodeDiscoverer);
    }

    @Override
    public void stop()
    {
        membershipManager.stop();
        
        super.stop();
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (currentTime < lastUpdateTime + 1000)
            return;
        
        lastUpdateTime = currentTime;
        
        if (flushManager.isFlushInProgress())
            return;
        if (pendingMemberships.isEmpty())
            return;
        
        MembershipInfo info = pendingMemberships.getFirst();
        for (Iterator<INode> it = info.joiningNodes.iterator(); it.hasNext(); )
        {
            INode node = it.next();
            if (nodeDiscoverer.getDiscoveredNodes().contains(node))
                it.remove();
        }
        
        if (!info.joiningNodes.isEmpty())
            return;
        
        pendingMemberships.removeFirst();
        
        flushManager.install(info.membership, info.membershipDelta);
    }
    
    public IGroupMembership findMembership(long id)
    {
        for (IGroupMembership membership : membershipHistory)
        {
            if (membership.getId() == id)
                return membership;
        }
        
        return null;
    }
    
    public void installGroupMembership(IGroup group)
    {
        Assert.notNull(group);
        Assert.isTrue(group.getId().equals(groupId));
        
        if (!group.getCoordinator().equals(membershipManager.getLocalNode()))
            return;
        
        Assert.checkState(!flushManager.isFlushInProgress());
        Assert.checkState(membershipManager.getMembership() == null);
        
        IGroupMembership membership = new GroupMembership(1, group);
        flushManager.install(membership, null);
    }
    
    public void installGroupMembership(IGroupChange changedGroup)
    {
        Assert.notNull(changedGroup);
        
        IGroup group = changedGroup.getNewGroup();
        Assert.isTrue(group.getId().equals(groupId));
        
        if (!group.getCoordinator().equals(membershipManager.getLocalNode()))
            return;
        
        IGroupMembership oldMembership = membershipManager.getMembership();
        if (oldMembership == null)
        {
            installGroupMembership(group);
            IDataLossState state = new DataLossState(group.getCoordinator().getDomain(), groupId);
            dataLossFeedbackService.updateDataLossState(state);
            return;
        }
        
        Set<UUID> leftNodes = new LinkedHashSet<UUID>();
        for (INode node : changedGroup.getLeftMembers())
        {
            if (oldMembership.getGroup().findMember(node.getId()) != null)
                leftNodes.add(node.getId());
        }
        
        Set<UUID> failedNodes = new LinkedHashSet<UUID>();
        for (INode node : oldMembership.getGroup().getMembers())
        {
            if (group.findMember(node.getId()) == null && !leftNodes.contains(node.getId()))
                failedNodes.add(node.getId());
        }
        
        List<INode> joiningNodes = new LinkedList<INode>();
        List<INode> joinedNodes = new ArrayList<INode>();
        for (INode node : group.getMembers())
        {
            if (oldMembership.getGroup().findMember(node.getId()) == null)
            {
                joiningNodes.add(node);
                joinedNodes.add(node);
            }
        }
        
        IGroupMembership newMembership = new GroupMembership(oldMembership.getId() + 1, group);
        IGroupDelta groupDelta = new GroupDelta(group.getId(), group.isPrimary(),
            joinedNodes, leftNodes, failedNodes);
        IGroupMembershipDelta membershipDelta = new GroupMembershipDelta(newMembership.getId(), groupDelta);
        
        MembershipInfo info = new MembershipInfo();
        info.membership = newMembership;
        info.membershipDelta = membershipDelta;
        info.joiningNodes = joiningNodes;
        pendingMemberships.addLast(info);
    }
    
    @Override
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership,
        IGroupMembershipChange change)
    {
        membershipHistory.addFirst(newMembership);
        if (membershipHistory.size() > maxGroupMembershipHistorySize)
            membershipHistory.removeLast();
    }

    @Override
    public void onJoined()
    {
        for (IClusterMembershipListener listener : clusterMembershipListeners)
            listener.onJoined();
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
        for (IClusterMembershipListener listener : clusterMembershipListeners)
            listener.onLeft(reason);
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        for (IClusterMembershipListener listener : clusterMembershipListeners)
            listener.onMembershipChanged(event);
    }
    
    @Override
    public void process()
    {
        if (!compartmentProcessors.isEmpty())
        {
            for (ICompartmentProcessor processor : compartmentProcessors)
                processor.process();
        }
    }

    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        super.doSend(sender, message.addPart(new GroupMessagePart(groupId)));
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        return super.doSend(feed, sink, message.addPart(new GroupMessagePart(groupId)));
    }
    
    private static class MembershipInfo
    {
        private IGroupMembership membership;
        private IGroupMembershipDelta membershipDelta;
        private List<INode> joiningNodes;
    }
}
