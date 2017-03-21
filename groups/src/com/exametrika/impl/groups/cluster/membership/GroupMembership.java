/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupMembership} is implementation of {@link IGroupMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembership implements IGroupMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final IGroup group;

    public GroupMembership(long id, IGroup group)
    {
        Assert.isTrue(id > 0);
        Assert.notNull(group);

        this.id = id;
        this.group = group;
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public IGroup getGroup()
    {
        return group;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof GroupMembership))
            return false;

        GroupMembership membership = (GroupMembership)o;
        return id == membership.id;
    }

    @Override
    public int hashCode()
    {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public String toString()
    {
        return messages.toString(id, group).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id : {0}\ngroup: {1}")
        ILocalizedMessage toString(long id, IGroup group);
    }
}
