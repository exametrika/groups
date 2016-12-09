/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.Set;

import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link MembershipChange} is implementation of {@link IMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipChange implements IMembershipChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<INode> joinedMembers;
    private final Set<INode> leftMembers;
    private final Set<INode> failedMembers;

    public MembershipChange(Set<INode> joinedMembers, Set<INode> leftMembers, Set<INode> failedMembers)
    {
        Assert.notNull(joinedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(failedMembers);
        
        this.joinedMembers = Immutables.wrap(joinedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.failedMembers = Immutables.wrap(failedMembers);
    }

    @Override
    public Set<INode> getJoinedMembers()
    {
        return joinedMembers;
    }
    
    @Override
    public Set<INode> getLeftMembers()
    {
        return leftMembers;
    }
    
    @Override
    public Set<INode> getFailedMembers()
    {
        return failedMembers;
    }
    
    @Override
    public String toString()
    {
        return messages.toString(joinedMembers, leftMembers, failedMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("joined: {0}\nleft: {1}\nfailed: {2}")
        ILocalizedMessage toString(Set<INode> joinedMembers, Set<INode> leftMembers, Set<INode> failedMembers);
    }
}
