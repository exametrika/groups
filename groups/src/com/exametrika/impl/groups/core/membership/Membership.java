/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link Membership} is implementation of {@link IMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Membership implements IMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final IGroup group;

    public Membership(long id, IGroup group)
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

        if (!(o instanceof Membership))
            return false;

        Membership member = (Membership)o;
        return id == member.id;
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
