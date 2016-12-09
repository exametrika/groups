/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link AcknowledgeRetransmitMessagePart} is a retransmit acknowledge message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AcknowledgeRetransmitMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID failedNodeId;
    private final long flushId;

    public AcknowledgeRetransmitMessagePart(UUID failedNodeId, long flushId)
    {
        Assert.notNull(failedNodeId);
        
        this.failedNodeId = failedNodeId; 
        this.flushId = flushId;
    }

    public UUID getFailedNodeId()
    {
        return failedNodeId;
    }
    
    public long getFlushId()
    {
        return flushId;
    }
    
    @Override
    public int getSize()
    {
        return 24;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(failedNodeId, flushId).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("failed node-id: {0}, flush-id: {1}")
        ILocalizedMessage toString(UUID failedNodeId, long flushId);
    }
}

