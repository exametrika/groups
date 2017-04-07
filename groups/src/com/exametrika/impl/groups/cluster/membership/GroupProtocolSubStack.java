/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolSubStack;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;

/**
 * The {@link GroupProtocolSubStack} represents a group protocol sub-stack.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class GroupProtocolSubStack extends ProtocolSubStack implements IPreparedGroupMembershipListener
{
    private final UUID groupId;
    private final GroupMembershipManager membershipManager;
    private final int maxGroupMembershipHistorySize;
    private long startRemoveTime;
    private final Deque<IGroupMembership> membershipHistory = new ArrayDeque<IGroupMembership>();
    private IFlushManager flushManager;

    public GroupProtocolSubStack(String channelName, IMessageFactory messageFactory, UUID groupId,
        List<? extends AbstractProtocol> protocols, GroupMembershipManager membershipManager, int maxGroupMembershipHistorySize)
    {
        super(channelName, messageFactory, protocols);
        
        Assert.notNull(groupId);
        Assert.notNull(membershipManager);
        
        this.groupId = groupId;
        this.membershipManager = membershipManager;
        this.maxGroupMembershipHistorySize = maxGroupMembershipHistorySize;
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
    }

    @Override
    public void stop()
    {
        membershipManager.stop();
        
        super.stop();
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
        Assert.isTrue(changedGroup.getNewGroup().getId().equals(groupId));
        
        if (!changedGroup.getNewGroup().getCoordinator().equals(membershipManager.getLocalNode()))
            return;
        
        Assert.checkState(!flushManager.isFlushInProgress());
        IGroupMembership oldMembership = membershipManager.getMembership();
        Assert.notNull(oldMembership);
        
        Set<UUID> leftMembers = new LinkedHashSet<UUID>();
        for (INode node : changedGroup.getLeftMembers())
            leftMembers.add(node.getId());
        
        Set<UUID> failedMembers = new LinkedHashSet<UUID>();
        for (INode node : changedGroup.getFailedMembers())
            failedMembers.add(node.getId());
        
        IGroupMembership newMembership = new GroupMembership(oldMembership.getId() + 1, changedGroup.getNewGroup());
        IGroupDelta groupDelta = new GroupDelta(changedGroup.getNewGroup().getId(), changedGroup.getNewGroup().isPrimary(),
            changedGroup.getJoinedMembers(), leftMembers, failedMembers);
        IGroupMembershipDelta membershipDelta = new GroupMembershipDelta(newMembership.getId(), groupDelta);
        
        flushManager.install(newMembership, membershipDelta);
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
    protected void doSend(ISender sender, IMessage message)
    {
        super.doSend(sender, message.addPart(new GroupMessagePart(groupId)));
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        return super.doSend(feed, sink, message.addPart(new GroupMessagePart(groupId)));
    }
}
