/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.Set;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link FailureUpdateMessagePart} is a failure update message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureUpdateMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;

    public FailureUpdateMessagePart(Set<UUID> failedMembers, Set<UUID> leftMembers)
    {
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
    }
    
    public Set<UUID> getFailedMembers()
    {
        return failedMembers;
    }

    public Set<UUID> getLeftMembers()
    {
        return leftMembers;
    }

    @Override
    public int getSize()
    {
        return failedMembers.size() * 16 + leftMembers.size() * 16;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(failedMembers, leftMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("failed members: {0}, left members: {1}")
        ILocalizedMessage toString(Set<UUID> failedMembers, Set<UUID> leftMembers);
    }
}

