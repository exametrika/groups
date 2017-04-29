/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.groups.cluster.flush.IFlushCondition;



/**
 * The {@link GroupMemberships} contains different utility methods for membership manipulations.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMemberships
{
    public static final UUID CORE_GROUP_ID = UUID.fromString("d2ea2d73-c7ee-492b-a532-7a81fc74fe8c");
    public static final String CORE_GROUP_NAME = "core"; 
    public static final String CORE_DOMAIN = "core"; 
    public static final GroupAddress CORE_GROUP_ADDRESS = new GroupAddress(CORE_GROUP_ID, CORE_GROUP_NAME);
    
    public static class MembershipDeltaInfo
    {
        public final IGroupMembership oldMembership;
        public final IGroupMembership newMembership;
        public final IGroupMembershipDelta membershipDelta;
        
        public MembershipDeltaInfo(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipDelta membershipDelta)
        {
            Assert.notNull(oldMembership);
            Assert.notNull(newMembership);
            Assert.notNull(membershipDelta);
            
            this.oldMembership = oldMembership;
            this.newMembership = newMembership;
            this.membershipDelta = membershipDelta;
        }
    }
    
    public static class MembershipChangeInfo
    {
        public final IGroupMembership oldMembership;
        public final IGroupMembership newMembership;
        public final IGroupMembershipChange membershipChange;
        
        public MembershipChangeInfo(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
        {
            Assert.notNull(oldMembership);
            Assert.notNull(newMembership);
            Assert.notNull(membershipChange);
            
            this.oldMembership = oldMembership;
            this.newMembership = newMembership;
            this.membershipChange = membershipChange;
        }
    }
    
    public static IGroupMembership createCoreMembership(INode localNode, Set<INode> discoveredNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(discoveredNodes);
        Assert.isTrue(!discoveredNodes.contains(localNode));
        
        List<INode> members = new ArrayList<INode>(discoveredNodes.size() + 1);
        members.add(localNode);
        members.addAll(discoveredNodes);

        IGroup group = new Group(CORE_GROUP_ADDRESS, true, members, Enums.of(GroupOption.DURABLE, GroupOption.ORDERED, 
            GroupOption.SIMPLE_STATE_TRANSFER));
        
        return new GroupMembership(1, group);
    }
    
    public static MembershipDeltaInfo createMembership(IGroupMembership oldMembership, Set<INode> failedMembers, Set<INode> leftMembers,
        Set<INode> discoveredNodes, IFlushCondition flushCondition)
    {
        Assert.notNull(oldMembership);
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(discoveredNodes);
        
        if (failedMembers.isEmpty() && leftMembers.isEmpty() && discoveredNodes.isEmpty())
            return null;

        boolean primaryGroup;
        if (oldMembership.getGroup().isPrimary())
            primaryGroup = isPrimary(oldMembership.getGroup().getMembers(), failedMembers, leftMembers);
        else
            primaryGroup = false;

        leftMembers = new LinkedHashSet<INode>(leftMembers);
        leftMembers.retainAll(oldMembership.getGroup().getMembers());
        
        Set<UUID> leftMemberIds = new LinkedHashSet<UUID>(leftMembers.size());
        for (INode member : leftMembers)
            leftMemberIds.add(member.getId());
        
        failedMembers = new LinkedHashSet<INode>(failedMembers);
        failedMembers.retainAll(oldMembership.getGroup().getMembers());
        
        Set<UUID> failedMemberIds = new LinkedHashSet<UUID>(failedMembers.size());
        for (INode member : failedMembers)
            failedMemberIds.add(member.getId());
        
        List<INode> members = new ArrayList<INode>(oldMembership.getGroup().getMembers());
        members.removeAll(leftMembers);
        members.removeAll(failedMembers);

        List<INode> joinedMembers = new ArrayList<INode>();
        for (INode node : discoveredNodes)
        {
            if (!members.contains(node))
            {
                members.add(node);
                joinedMembers.add(node);
            }
        }
        
        if (failedMemberIds.isEmpty() && leftMemberIds.isEmpty() && joinedMembers.isEmpty())
            return null;
        
        if (flushCondition != null && !flushCondition.canStartFlush(members, joinedMembers, failedMemberIds, leftMemberIds))
            return null;
        
        IGroup group = new Group((GroupAddress)oldMembership.getGroup().getAddress(), primaryGroup, members, 
            oldMembership.getGroup().getOptions());
        
        IGroupMembership newMembership = new GroupMembership(oldMembership.getId() + 1, group);
        IGroupMembershipDelta membershipDelta = new GroupMembershipDelta(newMembership.getId(), 
            new GroupDelta(group.getId(), group.isPrimary(), joinedMembers, leftMemberIds, failedMemberIds));

        return new MembershipDeltaInfo(oldMembership, newMembership, membershipDelta);
    }

    public static MembershipChangeInfo createMembership(IGroupMembership oldMembership, IGroupMembershipDelta membershipDelta)
    {
        Assert.notNull(oldMembership);
        Assert.notNull(membershipDelta);
        Assert.isTrue(membershipDelta.getId() == oldMembership.getId() + 1);
       
        List<INode> members = new ArrayList<INode>();
        Set<INode> failedMembers = new HashSet<INode>();
        Set<INode> leftMembers = new HashSet<INode>();
        for (INode member : oldMembership.getGroup().getMembers())
        {
            if (membershipDelta.getGroup().getFailedMembers().contains(member.getId()))
                failedMembers.add(member);
            else if (membershipDelta.getGroup().getLeftMembers().contains(member.getId()))
                leftMembers.add(member);
            else
                members.add(member);
        }
        
        List<INode> joinedMembers = membershipDelta.getGroup().getJoinedMembers();
        members.addAll(joinedMembers);
        
        boolean primaryGroup;
        if (oldMembership.getGroup().isPrimary())
            primaryGroup = isPrimary(oldMembership.getGroup().getMembers(), failedMembers, leftMembers);
        else
            primaryGroup = false;

        IGroup group = new Group((GroupAddress)oldMembership.getGroup().getAddress(), primaryGroup, members,
            oldMembership.getGroup().getOptions());
        
        IGroupMembership newMembership = new GroupMembership(oldMembership.getId() + 1, group);
        IGroupMembershipChange membershipChange = new GroupMembershipChange(new GroupChange(group, oldMembership.getGroup(), 
            joinedMembers, leftMembers, failedMembers));
        return new MembershipChangeInfo(oldMembership, newMembership, membershipChange);
    }
    
    private static boolean isPrimary(List<INode> members, Set<INode> groupFailedMembers, Set<INode> groupLeftMembers)
    {
        List<INode> healthyMembers = new ArrayList<INode>(members);
        healthyMembers.removeAll(groupFailedMembers);
        healthyMembers.removeAll(groupLeftMembers);
        
        List<INode> failedMembers = new ArrayList<INode>(members);
        failedMembers.retainAll(groupFailedMembers);
        failedMembers.removeAll(groupLeftMembers);
        
        if (healthyMembers.size() > failedMembers.size())
            return true;
        else if (healthyMembers.size() < failedMembers.size())
            return false;
        else if (!healthyMembers.isEmpty())
        {
            int h = members.indexOf(healthyMembers.get(0));
            int f = members.indexOf(failedMembers.get(0));
            
            if (h < f)
                return true;
            else
                return false;
        }
        else
            return false;
    }

    private GroupMemberships()
    {
    }
}
