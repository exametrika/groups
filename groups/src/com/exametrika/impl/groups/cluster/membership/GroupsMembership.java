/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupsMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link GroupsMembership} is implementation of {@link IGroupsMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupsMembership implements IGroupsMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IGroup> groups;
    private final Map<UUID, IGroup> groupsByIdMap;
    private final Map<IAddress, IGroup> groupsByAddressMap;
    private final Map<UUID, List<IGroup>> groupsByNodeMap;

    public GroupsMembership(List<IGroup> groups)
    {
        Assert.notNull(groups);

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
        return messages.toString(Strings.toString(groups, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("groups: \n{0}")
        ILocalizedMessage toString(String groups);
    }
}
