/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link GroupMembership} is implementation of {@link IGroupMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembership implements IGroupMembership
{
    private final List<IGroup> groups;
    private final Map<UUID, IGroup> groupsByIdMap;
    private final Map<IAddress, IGroup> groupsByAddressMap;
    private final Map<UUID, List<IGroup>> groupsByNodeMap;

    public GroupMembership(List<IGroup> groups)
    {
        Assert.notNull(groups);
        Assert.isTrue(!groups.isEmpty());

        this.groups = Immutables.wrap(groups);
        
        Map<UUID, IGroup> groupsByIdMap = new HashMap<UUID, IGroup>();
        Map<IAddress, IGroup> groupsByAddressMap = new HashMap<IAddress, IGroup>();
        Map<UUID, List<IGroup>> groupsByNodeMap = new HashMap<UUID, List<IGroup>>();
        for (IGroup group : groups)
        {
            groupsByIdMap.put(group.getId(), group);
            groupsByAddressMap.put(group.getAddress(), group);
            
            for (INode node : group.getMembers())
            {
                List<IGroup> list = groupsByNodeMap.get(node.getId());
                if (list == null)
                {
                    list = Immutables.wrap(new ArrayList<IGroup>());
                    groupsByNodeMap.put(node.getId(), list);
                }
                
                list = Immutables.unwrap(list);
                list.add(group);
            }
        }
        
        this.groupsByIdMap = groupsByIdMap;
        this.groupsByAddressMap = groupsByAddressMap;
        this.groupsByNodeMap = groupsByNodeMap;
    }

    @Override
    public List<IGroup> getGroups()
    {
        return groups;
    }

    @Override
    public IGroup findGroup(UUID groupId)
    {
        Assert.notNull(groupId);
        
        return groupsByIdMap.get(groupId);
    }

    @Override
    public List<IGroup> findNodeGroups(UUID nodeId)
    {
        Assert.notNull(nodeId);
        
        return groupsByNodeMap.get(nodeId);
    }
    
    @Override
    public IGroup findGroup(IAddress address)
    {
        Assert.notNull(address);
        
        return groupsByAddressMap.get(address);
    }

    @Override
    public String toString()
    {
        return Strings.toString(groups, false);
    }
}
