/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.management.ICommand;
import com.exametrika.impl.groups.cluster.management.ICommandHandler;

/**
 * The {@link SimpleGroupMappingStrategy} is implementation of {@link IGroupMappingStrategy}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleGroupMappingStrategy implements IGroupMappingStrategy, ICommandHandler
{
    private final Map<String, DomainInfo> domains = new LinkedHashMap<String, DomainInfo>();
    
    public List<GroupDefinition> getGroupDefinitions()
    {
        List<GroupDefinition> groups = new ArrayList<GroupDefinition>();
        for (DomainInfo domain : domains.values())
        {
            for (GroupDefinition group : domain.groupDefinitions.values())
                groups.add(group);
        }
        return groups;
    }
    
    public void addGroup(GroupDefinition group)
    {
        Assert.notNull(group);
        
        DomainInfo domain = domains.get(group.getDomain());
        if (domain == null)
        {
            domain = new DomainInfo();
            domains.put(group.getDomain(), domain);
        }
        domain.groupDefinitions.put(group.getId(), group);
        domain.changedGroups.add(group.getId());
    }
    
    public void removeGroup(String domainName, UUID groupId)
    {
        Assert.notNull(groupId);
        
        DomainInfo domain = domains.get(domainName);
        if (domain == null)
            return;
        
        if (domain.groupDefinitions.remove(groupId) != null)
            domain.removedGroups.add(groupId);
    }
    
    @Override
    public List<Pair<IGroup, IGroupDelta>> mapGroups(long membershipId, String domain, NodesMembership nodeMembership,
        NodesMembershipDelta nodesMembershipDelta, GroupsMembership oldGroupMembership)
    {
        // TODO: учитывать NodeMembershipDelta только если предыдущий вызов был на 1 меньше по membershipid (не было пропусков),
        // т.к. дельта относительно предыдущего
        DomainInfo domainInfo = domains.get(domain);
        if (domainInfo == null)
            return null;
        if (domainInfo.changedGroups.isEmpty() && domainInfo.removedGroups.isEmpty() && nodesMembershipDelta.getJoinedNodes().isEmpty() &&
            nodesMembershipDelta.getLeftNodes().isEmpty() && nodesMembershipDelta.getFailedNodes().isEmpty())
            return null;
        
        List<Pair<IGroup, IGroupDelta>> resultGroups = new ArrayList<Pair<IGroup, IGroupDelta>>();
        Map<UUID, ChangedGroupInfo> changedGroups = new LinkedHashMap<UUID, ChangedGroupInfo>();
        if (oldGroupMembership != null)
        {
            for (UUID nodeId : nodesMembershipDelta.getLeftNodes())
            {
                List<IGroup> groups = oldGroupMembership.findNodeGroups(nodeId);
                for (IGroup group : groups)
                    domainInfo.changedGroups.add(group.getId());
            }
            
            for (UUID nodeId : nodesMembershipDelta.getFailedNodes())
            {
                List<IGroup> groups = oldGroupMembership.findNodeGroups(nodeId);
                for (IGroup group : groups)
                    domainInfo.changedGroups.add(group.getId());
            }
            
            for (IGroup group : oldGroupMembership.getGroups())
            {
                if (!domainInfo.changedGroups.contains(group.getId()) && !domainInfo.removedGroups.contains(group.getId()))
                    resultGroups.add(new Pair<IGroup, IGroupDelta>(group, null));
                        
            }
        }
        
        for (UUID groupId : domainInfo.changedGroups)
        {
            ChangedGroupInfo changedGroup = new ChangedGroupInfo();
            changedGroup.definition = domainInfo.groupDefinitions.get(groupId);
            if (oldGroupMembership != null)
                changedGroup.group = oldGroupMembership.findGroup(groupId);
            if (changedGroup.group != null)
            
        }
        // TODO:
        // - найти группы удаленных вышедших и добавить их в набор измененных
        // - если если старое членство, пройти по всем его группам и добавить в список новых групп,
        //   если группа не в списке удаленных
        // - пройти по измененным, если не в старых, добавить в список новых групп
        // - очистить наборы измененных и удаленных
        // TODO: по каждой группе
        // - если группа не ищмененная и нет новых узлов включить группу как есть бещ дельты
        // - иначе смаппить группу
        // TODO:
        // - при маппинге учесть описание группы (включая кастомный селектор узлов (который не добавлен)
        // - graceful exit узлы (добавлять новый узел на каждый такой узел группы, исключать из группы такой узел,
        //   когда состояние группы подтверлит, чтоновые узлывключены в группу ифлаш завершен)
        // - состояние группы - не включать новые узлы пока флаш не отработает, удаленные и вышедшие включаем
        
        return resultGroups;
    }

    @Override
    public boolean supports(ICommand command)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void execute(ICommand command)
    {
        // TODO Auto-generated method stub
        // - add/remove group definition
    }
    
    private static class DomainInfo
    {
        private final Map<UUID, GroupDefinition> groupDefinitions = new LinkedHashMap<UUID, GroupDefinition>();
        private final Set<UUID> changedGroups = new LinkedHashSet<UUID>();
        private final Set<UUID> removedGroups = new LinkedHashSet<UUID>();
    }
    
    private static class ChangedGroupInfo
    {
        GroupDefinition definition;
        IGroup group;
        List<INode> nodes = new ArrayList<INode>();
        Set<UUID> failedNodes;
        Set<UUID> leftNodes;
        Set<UUID> gracefulExitNodes;
    }
}
