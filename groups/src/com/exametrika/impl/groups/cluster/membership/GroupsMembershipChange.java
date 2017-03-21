/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupsMembershipChange;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link GroupsMembershipChange} is implementation of {@link IGroupsMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupsMembershipChange implements IGroupsMembershipChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<IGroup> newGroups;
    private final Set<IGroupChange> changedGroups;
    private final Set<IGroup> removedGroups;

    public GroupsMembershipChange(Set<IGroup> newGroups, Set<IGroupChange> changedGroups, Set<IGroup> removedGroups)
    {
        Assert.notNull(newGroups);
        Assert.notNull(changedGroups);
        Assert.notNull(removedGroups);
        
        this.newGroups = Immutables.wrap(newGroups);
        this.changedGroups = Immutables.wrap(changedGroups);
        this.removedGroups = Immutables.wrap(removedGroups);
    }

    @Override
    public Set<IGroup> getNewGroups()
    {
        return newGroups;
    }
    
    @Override
    public Set<IGroupChange> getChangedGroups()
    {
        return changedGroups;
    }
    
    @Override
    public Set<IGroup> getRemovedGroups()
    {
        return removedGroups;
    }
    
    @Override
    public String toString()
    {
        return messages.toString(newGroups, changedGroups, removedGroups).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("new: {0}\nchanged: {1}\nremoved: {2}")
        ILocalizedMessage toString(Set<IGroup> newGroups, Set<IGroupChange> changedGroups, Set<IGroup> removedGroups);
    }
}
