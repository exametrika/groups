/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IGroupChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.core.membership.Group;
import com.exametrika.impl.groups.core.membership.GroupAddress;
import com.exametrika.impl.groups.core.membership.GroupChange;
import com.exametrika.impl.groups.core.membership.IGroupDelta;

/**
 * The {@link GroupMembershipProvider} is an implementation of group membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembershipProvider implements IClusterMembershipProvider
{
    private final IGroupMappingStrategy groupMappingStrategy;

    public GroupMembershipProvider(IGroupMappingStrategy groupMappingStrategy)
    {
        Assert.notNull(groupMappingStrategy);
        
        this.groupMappingStrategy = groupMappingStrategy;
    }
    
    @Override
    public boolean isCoreGroupOnly()
    {
        return false;
    }

    @Override
    public Set<String> getDomains()
    {
        return Collections.emptySet();
    }

    @Override
    public Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(
        IDomainMembership newDomainMembership, IDomainMembershipDelta domainMembershipDelta,
        IDomainMembership oldDomainMembership, IClusterMembershipElement oldMembership)
    {
        List<IGroup> oldGroups = null;
        if (oldMembership != null)
        {
            GroupMembership oldGroupMembership = (GroupMembership)oldMembership;
            oldGroups = oldGroupMembership.getGroups();
        }
        NodeMembership nodeMembership = newDomainMembership.findElement(NodeMembership.class);
        List<Pair<IGroup, IGroupDelta>> list = groupMappingStrategy.mapGroups(newDomainMembership.getName(),
            nodeMembership.getNodes(), oldGroups);
        
        List<IGroup> groups = new ArrayList<IGroup>();
        Set<IGroup> newGroups = new HashSet<IGroup>();
        Set<UUID> removedGroups = new HashSet<UUID>();
        Set<IGroupDelta> changedGroups = new HashSet<IGroupDelta>();
        for (Pair<IGroup, IGroupDelta> pair : list)
        {
            IGroup group = pair.getKey();
            IGroupDelta delta = pair.getValue();
            groups.add(group);
            if (delta != null)
                changedGroups.add(delta);
        }
        
        GroupMembership newGroupMembership = new GroupMembership(groups);
        if (oldGroups != null)
        {
            for (IGroup group : oldGroups)
            {
                if (newGroupMembership.findGroup(group.getId()) == null)
                    removedGroups.add(group.getId());
            }
        }
        
        if (newGroups.isEmpty() && changedGroups.isEmpty() && removedGroups.isEmpty())
            return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(oldMembership, null);
            
        GroupMembershipDelta newGroupMembershipDelta = new GroupMembershipDelta(newGroups, changedGroups, removedGroups);
        return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(newGroupMembership, newGroupMembershipDelta);
    }

    @Override
    public void clearState()
    {
    }

    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        return new GroupMembershipDelta(Collections.<IGroup>emptySet(), Collections.<IGroupDelta>emptySet(), 
            Collections.<UUID>emptySet());
    }

    @Override
    public boolean isEmptyMembership(IClusterMembershipElement membership)
    {
        GroupMembership groupMembership = (GroupMembership)membership;
        return groupMembership.getGroups().isEmpty();
    }

    @Override
    public IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership)
    {
        GroupMembership groupMembership = (GroupMembership)membership;
        return new GroupMembershipDelta(new LinkedHashSet<IGroup>(groupMembership.getGroups()), Collections.<IGroupDelta>emptySet(), 
            Collections.<UUID>emptySet());
    }

    @Override
    public IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership,
        IClusterMembershipElementDelta delta, boolean full, boolean publicPart)
    {
        if (publicPart)
            return createEmptyDelta();
        
        if (full)
            return createCoreFullDelta(membership);
        else
        {
            Assert.notNull(delta);
            return delta;
        }
    }

    @Override
    public IClusterMembershipElement createMembership(IDomainMembership newDomainMembership,
        IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        
        GroupMembershipDelta groupDelta = (GroupMembershipDelta)delta;
        GroupMembership oldGroupMembership = (GroupMembership)oldMembership;
        if (oldMembership == null)
            return new GroupMembership(new ArrayList<>(groupDelta.getNewGroups()));
        else
        {
            List<IGroup> groups = new ArrayList<IGroup>();
            Map<UUID, IGroupDelta> changedGroups = new LinkedHashMap<UUID, IGroupDelta>();
            for (IGroupDelta changedGroup : groupDelta.getChangedGroups())
                changedGroups.put(changedGroup.getId(), changedGroup);
            
            for (IGroup group : oldGroupMembership.getGroups())
            {
                if (groupDelta.getRemovedGroups().contains(group.getId()))
                    continue;
               
                IGroupDelta changedGroup = changedGroups.get(group.getId());
                if (changedGroup != null)
                {
                    List<INode> members = new ArrayList<INode>();
                    for (INode node : group.getMembers())
                    {
                        if (!changedGroup.getFailedMembers().contains(node.getId()) && !changedGroup.getLeftMembers().contains(node.getId()))
                            members.add(node);
                    }
                    
                    members.addAll(changedGroup.getJoinedMembers());
                    group = new Group((GroupAddress)group.getAddress(), changedGroup.isPrimary(), members);
                }
                
                groups.add(group); 
            }
            
            groups.addAll(groupDelta.getNewGroups());
            
            return new GroupMembership(groups);
        }
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        Assert.notNull(oldMembership);
        
        GroupMembershipDelta groupDelta = (GroupMembershipDelta)delta;
        GroupMembership oldGroupMembership = (GroupMembership)oldMembership;
        GroupMembership newGroupMembership = newDomainMembership.findElement(GroupMembership.class);
  
        Map<UUID, IGroupDelta> changedGroupsMap = new LinkedHashMap<UUID, IGroupDelta>();
        for (IGroupDelta changedGroup : groupDelta.getChangedGroups())
            changedGroupsMap.put(changedGroup.getId(), changedGroup);
        
        Set<IGroup> removedGroups = new HashSet<IGroup>();
        Set<IGroupChange> changedGroups = new HashSet<IGroupChange>();
        for (IGroup group : oldGroupMembership.getGroups())
        {
            if (groupDelta.getRemovedGroups().contains(group.getId()))
                removedGroups.add(group);
            else if (changedGroupsMap.containsKey(group.getId()))
            {
                IGroupDelta changedGroup = changedGroupsMap.get(group.getId());
                IGroup newGroup = newGroupMembership.findGroup(group.getId());
                Set<INode> failedMembers = new HashSet<INode>();
                Set<INode> leftMembers = new HashSet<INode>();
                for (UUID nodeId : changedGroup.getFailedMembers())
                {
                    INode failedMember = group.findMember(nodeId);
                    Assert.notNull(failedMember);
                    failedMembers.add(failedMember);
                }
                for (UUID nodeId : changedGroup.getLeftMembers())
                {
                    INode leftMember = group.findMember(nodeId);
                    Assert.notNull(leftMember);
                    leftMembers.add(leftMember);
                }
                IGroupChange groupChange = new GroupChange(newGroup, group, new LinkedHashSet<>(changedGroup.getJoinedMembers()),
                    leftMembers, failedMembers);
                changedGroups.add(groupChange);
            }
        }
        
        return new GroupMembershipChange(groupDelta.getNewGroups(), changedGroups, removedGroups);
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElement newMembership, IClusterMembershipElement oldMembership)
    {
        Assert.notNull(newMembership);
        Assert.notNull(oldMembership);
        
        GroupMembership newGroupMembership = (GroupMembership)newMembership;
        GroupMembership oldGroupMembership = (GroupMembership)oldMembership;
        
        Set<IGroup> removedGroups = new HashSet<IGroup>();
        Set<IGroupChange> changedGroups = new HashSet<IGroupChange>();
        for (IGroup group : oldGroupMembership.getGroups())
        {
            IGroup newGroup = newGroupMembership.findGroup(group.getId());
            if (newGroup == null)
                removedGroups.add(group);
            else
            {
                Set<INode> failedNodes = new HashSet<INode>();
                for (INode node : group.getMembers())
                {
                    if (newGroup.findMember(node.getId()) == null)
                        failedNodes.add(node);
                }
                
                Set<INode> joinedNodes = new HashSet<INode>();
                for (INode node : newGroup.getMembers())
                {
                    if (group.findMember(node.getId()) == null)
                        joinedNodes.add(node);
                }
                if (!joinedNodes.isEmpty() || !failedNodes.isEmpty() || group.isPrimary() != newGroup.isPrimary())
                {
                    IGroupChange changedGroup = new GroupChange(newGroup, group, joinedNodes, Collections.<INode>emptySet(), failedNodes);
                    changedGroups.add(changedGroup);
                }
            }
        }
        
        Set<IGroup> newGroups = new HashSet<IGroup>();
        for (IGroup group : newGroupMembership.getGroups())
        {
            if (oldGroupMembership.findGroup(group.getId()) == null)
                newGroups.add(group);
        }
        return new GroupMembershipChange(newGroups, changedGroups, removedGroups);
    }
}
