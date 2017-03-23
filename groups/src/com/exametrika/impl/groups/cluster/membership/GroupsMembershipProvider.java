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
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link GroupsMembershipProvider} is an implementation of groups membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupsMembershipProvider implements IClusterMembershipProvider
{
    private final IGroupMappingStrategy groupMappingStrategy;

    public GroupsMembershipProvider(IGroupMappingStrategy groupMappingStrategy)
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
            GroupsMembership oldGroupMembership = (GroupsMembership)oldMembership;
            oldGroups = oldGroupMembership.getGroups();
        }
        NodesMembership nodeMembership = newDomainMembership.findElement(NodesMembership.class);
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
        
        GroupsMembership newGroupMembership = new GroupsMembership(groups);
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
            
        GroupsMembershipDelta newGroupMembershipDelta = new GroupsMembershipDelta(newGroups, changedGroups, removedGroups);
        return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(newGroupMembership, newGroupMembershipDelta);
    }

    @Override
    public void clearState()
    {
    }

    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        return new GroupsMembershipDelta(Collections.<IGroup>emptySet(), Collections.<IGroupDelta>emptySet(), 
            Collections.<UUID>emptySet());
    }

    @Override
    public boolean isEmptyMembership(IClusterMembershipElement membership)
    {
        GroupsMembership groupMembership = (GroupsMembership)membership;
        return groupMembership.getGroups().isEmpty();
    }

    @Override
    public IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership)
    {
        GroupsMembership groupMembership = (GroupsMembership)membership;
        return new GroupsMembershipDelta(new LinkedHashSet<IGroup>(groupMembership.getGroups()), Collections.<IGroupDelta>emptySet(), 
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
        
        GroupsMembershipDelta groupDelta = (GroupsMembershipDelta)delta;
        GroupsMembership oldGroupMembership = (GroupsMembership)oldMembership;
        if (oldMembership == null)
            return new GroupsMembership(new ArrayList<>(groupDelta.getNewGroups()));
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
            
            return new GroupsMembership(groups);
        }
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        Assert.notNull(oldMembership);
        
        GroupsMembershipDelta groupDelta = (GroupsMembershipDelta)delta;
        GroupsMembership oldGroupMembership = (GroupsMembership)oldMembership;
        GroupsMembership newGroupMembership = newDomainMembership.findElement(GroupsMembership.class);
  
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
                IGroupChange groupChange = new GroupChange(newGroup, group, changedGroup.getJoinedMembers(),
                    leftMembers, failedMembers);
                changedGroups.add(groupChange);
            }
        }
        
        return new GroupsMembershipChange(groupDelta.getNewGroups(), changedGroups, removedGroups);
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElement newMembership, IClusterMembershipElement oldMembership)
    {
        Assert.notNull(newMembership);
        Assert.notNull(oldMembership);
        
        GroupsMembership newGroupMembership = (GroupsMembership)newMembership;
        GroupsMembership oldGroupMembership = (GroupsMembership)oldMembership;
        
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
                
                List<INode> joinedNodes = new ArrayList<INode>();
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
        return new GroupsMembershipChange(newGroups, changedGroups, removedGroups);
    }
}
