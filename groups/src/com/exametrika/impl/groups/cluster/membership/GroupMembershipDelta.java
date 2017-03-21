/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupMembershipDelta} is implementation of {@link IGroupMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembershipDelta implements IGroupMembershipDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final IGroupDelta group;

    public GroupMembershipDelta(long id, IGroupDelta group)
    {
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
    public IGroupDelta getGroup()
    {
        return group;
    }

    @Override
    public String toString()
    {
        return messages.toString(id, group).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id: {0}, {1}")
        ILocalizedMessage toString(long id, IGroupDelta group);
    }
}
