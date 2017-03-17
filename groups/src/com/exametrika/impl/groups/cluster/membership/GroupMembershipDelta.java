/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.membership.IGroupDelta;

/**
 * The {@link GroupMembershipDelta} is implementation of {@link IClusterMembershipElementDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembershipDelta implements IClusterMembershipElementDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<IGroup> newGroups;
    private final Set<IGroupDelta> changedGroups;
    private final Set<UUID> removedGroups;

    public GroupMembershipDelta(Set<IGroup> newGroups, Set<IGroupDelta> changedGroups, Set<UUID> removedGroups)
    {
        Assert.notNull(newGroups);
        Assert.notNull(changedGroups);
        Assert.notNull(removedGroups);
        
        this.newGroups = Immutables.wrap(newGroups);
        this.changedGroups = Immutables.wrap(changedGroups);
        this.removedGroups = Immutables.wrap(removedGroups);
    }

    public Set<IGroup> getNewGroups()
    {
        return newGroups;
    }
    
    public Set<IGroupDelta> getChangedGroups()
    {
        return changedGroups;
    }
    
    public Set<UUID> getRemovedGroups()
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
        ILocalizedMessage toString(Set<IGroup> newGroups, Set<IGroupDelta> changedGroups, Set<UUID> removedGroups);
    }
}
