/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

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
    private final List<INode> joinedMembers;
    private final Set<UUID> leftMembers;
    private final Set<UUID> failedMembers;

    public MembershipDelta(long id, List<INode> joinedMembers, Set<UUID> leftMembers, Set<UUID> failedMembers)
    {
        Assert.notNull(joinedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(failedMembers);
        
        this.id = id;
        this.joinedMembers = Immutables.wrap(joinedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.failedMembers = Immutables.wrap(failedMembers);
    }

    @Override
    public long getId()
    {
        return id;
    }
    
    @Override
    public List<INode> getJoinedMembers()
    {
        return joinedMembers;
    }
    
    @Override
    public Set<UUID> getLeftMembers()
    {
        return leftMembers;
    }
    
    @Override
    public Set<UUID> getFailedMembers()
    {
        return failedMembers;
    }

    @Override
    public String toString()
    {
        return messages.toString(id, joinedMembers, leftMembers, failedMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id: {0}, joined: {1}\nleft: {2}\nfailed: {3}")
        ILocalizedMessage toString(long id, List<INode> joinedMembers, Set<UUID> leftMembers, Set<UUID> failedMembers);
    }
}
