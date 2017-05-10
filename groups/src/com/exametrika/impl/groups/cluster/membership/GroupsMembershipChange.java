/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupsMembershipChange;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link GroupsMembershipChange} is implementation of {@link IGroupsMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupsMembershipChange implements IGroupsMembershipChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IGroup> newGroups;
    private final Set<IGroupChange> changedGroups;
    private final Map<UUID, IGroupChange> changedGroupsMap;
    private final Set<IGroup> removedGroups;

    public GroupsMembershipChange(List<IGroup> newGroups, Set<IGroupChange> changedGroups, Set<IGroup> removedGroups)
    {
        Assert.notNull(newGroups);
        Assert.notNull(changedGroups);
        Assert.notNull(removedGroups);
        
        this.newGroups = Immutables.wrap(newGroups);
        this.changedGroups = Immutables.wrap(changedGroups);
        this.removedGroups = Immutables.wrap(removedGroups);
        
        Map<UUID, IGroupChange> changedGroupsMap = new HashMap<UUID, IGroupChange>();
        for (IGroupChange group : changedGroups)
            changedGroupsMap.put(group.getNewGroup().getId(), group);
        this.changedGroupsMap = changedGroupsMap;
    }

    @Override
    public List<IGroup> getNewGroups()
    {
        return newGroups;
    }
    
    @Override
    public Set<IGroupChange> getChangedGroups()
    {
        return changedGroups;
    }
    
    @Override
    public  IGroupChange findChangedGroup(UUID groupId)
    {
        Assert.notNull(groupId);
        
        return changedGroupsMap.get(groupId);
    }
    
    @Override
    public Set<IGroup> getRemovedGroups()
    {
        return removedGroups;
    }
    
    @Override
    public String toString()
    {
        return messages.toString(Strings.toString(newGroups, true), Strings.toString(changedGroups, true), 
            Strings.toString(removedGroups, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("new groups: \n{0}\nchanged groups: \n{1}\nremoved groups: \n{2}")
        ILocalizedMessage toString(String newGroups, String changedGroups, String removedGroups);
    }
}
