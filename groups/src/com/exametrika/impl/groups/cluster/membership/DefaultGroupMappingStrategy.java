/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.feedback.IGroupFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.IGroupState;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.INodeState;
import com.exametrika.impl.groups.cluster.feedback.INodeState.State;
import com.exametrika.impl.groups.cluster.management.ICommand;
import com.exametrika.impl.groups.cluster.management.ICommandHandler;

/**
 * The {@link DefaultGroupMappingStrategy} is a default implementation of {@link IGroupMappingStrategy}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DefaultGroupMappingStrategy implements IGroupMappingStrategy, ICommandHandler, IGroupManagementService
{
    // TODO: провалидировать опции группы
    private final IGroupFeedbackService groupFeedbackService;
    private final INodeFeedbackService nodeFeedbackService;
    private final Map<String, DomainInfo> domains = new LinkedHashMap<String, DomainInfo>();
    
    public DefaultGroupMappingStrategy(IGroupFeedbackService groupFeedbackService, INodeFeedbackService nodeFeedbackService)
    {
        Assert.notNull(groupFeedbackService);
        Assert.notNull(nodeFeedbackService);
        
        this.groupFeedbackService = groupFeedbackService;
        this.nodeFeedbackService = nodeFeedbackService;
    }
    
    @Override
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
    
    @Override
    public void addGroupDefinition(GroupDefinition group)
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
    
    @Override
    public void removeGroupDefinition(String domainName, UUID groupId)
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
        Set<UUID> gracefulExitNodes = new HashSet<UUID>();
        if (oldGroupMembership != null)
        {
            for (UUID nodeId : nodesMembershipDelta.getLeftNodes())
            {
                List<IGroup> groups = oldGroupMembership.findNodeGroups(nodeId);
                for (IGroup group : groups)
                {
                    domainInfo.changedGroups.add(group.getId());
                    ChangedGroupInfo changedGroup = ensureChangedGroup(domainInfo, changedGroups, group.getId(), group);
                    changedGroup.leftNodes.add(nodeId);
                }
            }
            
            for (UUID nodeId : nodesMembershipDelta.getFailedNodes())
            {
                List<IGroup> groups = oldGroupMembership.findNodeGroups(nodeId);
                for (IGroup group : groups)
                {
                    domainInfo.changedGroups.add(group.getId());
                    ChangedGroupInfo changedGroup = ensureChangedGroup(domainInfo, changedGroups, group.getId(), group);
                    changedGroup.failedNodes.add(nodeId);
                }
            }
            
            for (IGroup group : oldGroupMembership.getGroups())
            {
                if (!domainInfo.changedGroups.contains(group.getId()) && !domainInfo.removedGroups.contains(group.getId()))
                    resultGroups.add(new Pair<IGroup, IGroupDelta>(group, null));
                        
            }
            
            for (INodeState nodeState : nodeFeedbackService.getNodeStates())
            {
                if (nodeState.getState() == State.GRACEFUL_EXIT_REQUESTED && nodeState.getDomain().equals(domain))
                {
                    gracefulExitNodes.add(nodeState.getId());
                    
                    List<IGroup> groups = oldGroupMembership.findNodeGroups(nodeState.getId());
                    for (IGroup group : groups)
                    {
                        domainInfo.changedGroups.add(group.getId());
                        ChangedGroupInfo changedGroup = ensureChangedGroup(domainInfo, changedGroups, group.getId(), group);
                        changedGroup.gracefulExitNodes.add(nodeState.getId());
                    }
                }
            }
        }
        
        for (UUID groupId : domainInfo.changedGroups)
        {
            ChangedGroupInfo changedGroup = ensureChangedGroup(domainInfo, changedGroups, groupId, null);
            if (changedGroup.group != null)
            {
                for (INode node : changedGroup.group.getMembers())
                {
                    if (!changedGroup.failedNodes.contains(node.getId()) && ! changedGroup.leftNodes.contains(node.getId()))
                        changedGroup.nodes.add(node);
                }
            }
            
            int nodeCount = changedGroup.nodes.size() - changedGroup.gracefulExitNodes.size();
            if (nodeCount >= changedGroup.definition.getNodeCount())
            {
                while (changedGroup.nodes.size() > changedGroup.definition.getNodeCount())
                {
                    // TODO: УДАЛИТЬ gracefulexit потом остальные лишниЕ
                }
            }
            else if (changedGroup.groupState.getState() == IGroupState.State.NORMAL)
            {
                int requiredNodesCount = changedGroup.definition.getNodeCount() - nodeCount;
            }
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
        return command instanceof AddGroupsCommand || command instanceof RemoveGroupsCommand;
    }

    @Override
    public void execute(ICommand command)
    {
        if (command instanceof AddGroupsCommand)
        {
            AddGroupsCommand addGroupsCommand = (AddGroupsCommand)command;
            for (GroupDefinition groupDefinition : addGroupsCommand.getGroupDefinitions())
                addGroupDefinition(groupDefinition);
        }
        else if (command instanceof AddGroupsCommand)
        {
            RemoveGroupsCommand removeGroupsCommand = (RemoveGroupsCommand)command;
            for (Pair<String, UUID> group : removeGroupsCommand.getGroups())
                removeGroupDefinition(group.getKey(), group.getValue());
        }
    }
    
    private ChangedGroupInfo ensureChangedGroup(DomainInfo domain, Map<UUID, ChangedGroupInfo> changedGroups, UUID groupId, IGroup group)
    {
        ChangedGroupInfo changedGroup = changedGroups.get(groupId);
        if (changedGroup == null)
        {
            changedGroup = new ChangedGroupInfo();
            changedGroups.put(group.getId(), changedGroup);
        }   
        
        if (group != null)
            changedGroup.group = group;
        
        if (changedGroup.definition == null)
        {
            changedGroup.definition = domain.groupDefinitions.get(groupId);
            changedGroup.groupState = groupFeedbackService.findGroupState(groupId);
        }
        
        return changedGroup;
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
        IGroupState groupState;
        List<INode> nodes = new LinkedList<INode>();
        Set<UUID> failedNodes = new HashSet<UUID>();
        Set<UUID> leftNodes = new HashSet<UUID>();
        Set<UUID> gracefulExitNodes = new HashSet<UUID>();
    }
}
