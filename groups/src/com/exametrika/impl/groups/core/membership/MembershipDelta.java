/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MembershipDelta} is implementation of {@link IMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipDelta implements IMembershipDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final IGroupDelta group;

    public MembershipDelta(long id, IGroupDelta group)
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
