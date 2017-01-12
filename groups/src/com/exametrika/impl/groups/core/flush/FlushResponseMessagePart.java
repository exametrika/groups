/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.Set;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link FlushResponseMessagePart} is a flush response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-Anse 
 */

public final class FlushResponseMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean flushProcessingRequired;
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;

    public FlushResponseMessagePart(boolean flushProcessingRequired, Set<UUID> failedMembers, Set<UUID> leftMembers)
    {
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        
        this.flushProcessingRequired = flushProcessingRequired;
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
    }
    
    public boolean isFlushProcessingRequired()
    {
        return flushProcessingRequired;
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
        return (failedMembers.size() + leftMembers.size()) * 16 + 1;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(flushProcessingRequired, failedMembers, leftMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("flush processing required: {0}, failed members: {1}, failed members: {2}")
        ILocalizedMessage toString(boolean flushProcessingRequired, Set<UUID> failedMembers, Set<UUID> leftMembers);
    }
}

