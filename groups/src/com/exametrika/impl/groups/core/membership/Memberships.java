/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.flush.IFlushCondition;



/**
 * The {@link Memberships} contains different utility methods for membership manipulations.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Memberships
{
    public static final UUID CORE_GROUP_ID = UUID.fromString("d2ea2d73-c7ee-492b-a532-7a81fc74fe8c");
    public static final String CORE_GROUP_NAME = "core"; 
    public static final String CORE_DOMAIN = "core"; 
    public static final GroupAddress CORE_GROUP_ADDRESS = new GroupAddress(CORE_GROUP_ID, CORE_GROUP_NAME);
    
    public static class MembershipDeltaInfo
    {
        public final IMembership oldMembership;
        public final IMembership newMembership;
        public final IMembershipDelta membershipDelta;
        
        public MembershipDeltaInfo(IMembership oldMembership, IMembership newMembership, IMembershipDelta membershipDelta)
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
        public final IMembership oldMembership;
        public final IMembership newMembership;
        public final IMembershipChange membershipChange;
        
        public MembershipChangeInfo(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
        {
            Assert.notNull(oldMembership);
            Assert.notNull(newMembership);
            Assert.notNull(membershipChange);
            
            this.oldMembership = oldMembership;
            this.newMembership = newMembership;
            this.membershipChange = membershipChange;
        }
    }
    
    public static IMembership createMembership(INode localNode, Set<INode> discoveredNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(discoveredNodes);
        Assert.isTrue(!discoveredNodes.contains(localNode));
        
        List<INode> members = new ArrayList<INode>(discoveredNodes.size() + 1);
        members.add(localNode);
        members.addAll(discoveredNodes);

        IGroup group = new Group(CORE_GROUP_ADDRESS, true, members);
        
        return new Membership(1, group);
    }
    
    public static MembershipDeltaInfo createMembership(IMembership oldMembership, Set<INode> failedMembers, Set<INode> leftMembers,
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
        
        IGroup group = new Group((GroupAddress)oldMembership.getGroup().getAddress(), primaryGroup, members);
        
        IMembership newMembership = new Membership(oldMembership.getId() + 1, group);
        IMembershipDelta membershipDelta = new MembershipDelta(newMembership.getId(), joinedMembers, leftMemberIds, 
            failedMemberIds);

        return new MembershipDeltaInfo(oldMembership, newMembership, membershipDelta);
    }

    public static MembershipChangeInfo createMembership(IMembership oldMembership, IMembershipDelta membershipDelta)
    {
        Assert.notNull(oldMembership);
        Assert.notNull(membershipDelta);
        Assert.isTrue(membershipDelta.getId() == oldMembership.getId() + 1);
       
        List<INode> members = new ArrayList<INode>();
        Set<INode> failedMembers = new HashSet<INode>();
        Set<INode> leftMembers = new HashSet<INode>();
        for (INode member : oldMembership.getGroup().getMembers())
        {
            if (membershipDelta.getFailedMembers().contains(member.getId()))
                failedMembers.add(member);
            else if (membershipDelta.getLeftMembers().contains(member.getId()))
                leftMembers.add(member);
            else
                members.add(member);
        }
        
        Set<INode> joinedMembers = new HashSet<INode>(membershipDelta.getJoinedMembers());
        members.addAll(membershipDelta.getJoinedMembers());
        
        boolean primaryGroup;
        if (oldMembership.getGroup().isPrimary())
            primaryGroup = isPrimary(oldMembership.getGroup().getMembers(), failedMembers, leftMembers);
        else
            primaryGroup = false;

        IGroup group = new Group((GroupAddress)oldMembership.getGroup().getAddress(), primaryGroup, members);
        
        IMembership newMembership = new Membership(oldMembership.getId() + 1, group);
        IMembershipChange membershipChange = new MembershipChange(joinedMembers, leftMembers, failedMembers);
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

    private Memberships()
    {
    }
}
