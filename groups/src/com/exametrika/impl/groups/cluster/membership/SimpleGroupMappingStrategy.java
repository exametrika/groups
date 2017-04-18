/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link SimpleGroupMappingStrategy} is implementation of {@link IGroupMappingStrategy}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleGroupMappingStrategy implements IGroupMappingStrategy
{
    private final Map<String, GroupDefinition> groupDefinitions = new LinkedHashMap<String, GroupDefinition>();
    private final Set<String> modifiedGroups = new LinkedHashSet<String>();
    private final Set<String> removedGroups = new LinkedHashSet<String>();
    
    public void addGroup(GroupDefinition group)
    {
        Assert.notNull(group);
        
        groupDefinitions.put(group.getName(), group);
        modifiedGroups.add(group.getName());
    }
    
    public void removeGroup(String groupName)
    {
        Assert.notNull(groupName);
        
        if (groupDefinitions.remove(groupName) != null)
            removedGroups.add(groupName);
    }
    
    @Override
    public List<Pair<IGroup, IGroupDelta>> mapGroups(String domain, NodesMembership nodeMembership,
        NodesMembershipDelta nodesMembershipDelta, GroupsMembership oldGroupMembership)
    {
        if (modifiedGroups.isEmpty() && removedGroups.isEmpty() && nodesMembershipDelta.getJoinedNodes().isEmpty() &&
            nodesMembershipDelta.getLeftNodes().isEmpty() && nodesMembershipDelta.getFailedNodes().isEmpty())
            return null;
        
        
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
        
        return null;
    }
}
