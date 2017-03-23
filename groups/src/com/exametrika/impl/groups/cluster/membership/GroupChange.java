/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link GroupChange} is implementation of {@link IGroupChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupChange implements IGroupChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroup newGroup;
    private final IGroup oldGroup;
    private final List<INode> joinedMembers;
    private final Set<INode> leftMembers;
    private final Set<INode> failedMembers;

    public GroupChange(IGroup newGroup, IGroup oldGroup, List<INode> joinedMembers, Set<INode> leftMembers, Set<INode> failedMembers)
    {
        Assert.notNull(joinedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(failedMembers);
        Assert.notNull(newGroup);
        Assert.notNull(oldGroup);
        
        this.newGroup = newGroup;
        this.oldGroup = oldGroup;
        this.joinedMembers = Immutables.wrap(joinedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.failedMembers = Immutables.wrap(failedMembers);
    }

    @Override
    public IGroup getNewGroup()
    {
        return newGroup;
    }
    
    @Override
    public IGroup getOldGroup()
    {
        return oldGroup;
    }
    
    @Override
    public List<INode> getJoinedMembers()
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
        return messages.toString(newGroup.getName(), joinedMembers, leftMembers, failedMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("{0} - joined: {1}\nleft: {2}\nfailed: {3}")
        ILocalizedMessage toString(String name, List<INode> joinedMembers, Set<INode> leftMembers, Set<INode> failedMembers);
    }
}
