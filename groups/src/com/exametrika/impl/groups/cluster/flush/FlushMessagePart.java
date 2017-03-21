/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

import java.util.Set;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link FlushMessagePart} is a flush message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Type type;
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;

    public enum Type
    {
        PROCESS,
        END,
        REQUEST_STATE
    }
    
    public FlushMessagePart(Type type, Set<UUID> failedMembers, Set<UUID> leftMembers)
    {
        Assert.notNull(type);
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        
        this.type = type;
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
    }
    
    public Type getType()
    {
        return type;
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
        return failedMembers.size() * 16 + leftMembers.size() * 16 + 1;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(type, failedMembers, leftMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("type: {0}, failed members: {1}, left members: {2}")
        ILocalizedMessage toString(Type type, Set<UUID> failedMembers, Set<UUID> leftMembers);
    }
}

