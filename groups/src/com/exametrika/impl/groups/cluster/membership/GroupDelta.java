/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link GroupDelta} is implementation of {@link IGroupDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDelta implements IGroupDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID id;
    private final boolean primary;
    private final List<INode> joinedMembers;
    private final Set<UUID> leftMembers;
    private final Set<UUID> failedMembers;

    public GroupDelta(UUID id, boolean primary, List<INode> joinedMembers, Set<UUID> leftMembers, Set<UUID> failedMembers)
    {
        Assert.notNull(id);
        Assert.notNull(joinedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(failedMembers);
        
        this.id = id;
        this.primary = primary;
        this.joinedMembers = Immutables.wrap(joinedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.failedMembers = Immutables.wrap(failedMembers);
    }

    @Override
    public UUID getId()
    {
        return id;
    }
    
    @Override
    public boolean isPrimary()
    {
        return primary;
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
        return messages.toString(id, primary, joinedMembers, leftMembers, failedMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id: {0}, primary: {1}, joined: {2}\nleft: {3}\nfailed: {4}")
        ILocalizedMessage toString(UUID id, boolean primary, List<INode> joinedMembers, Set<UUID> leftMembers, Set<UUID> failedMembers);
    }
}
