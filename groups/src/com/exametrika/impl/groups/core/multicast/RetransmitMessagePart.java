/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link RetransmitMessagePart} is a retransmit message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RetransmitMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID failedNodeId;
    private final long flushId;
    private final List<IMessage> retransmittedMessages;
    private final int size;

    public RetransmitMessagePart(UUID failedNodeId, long flushId, List<IMessage> retransmittedMessages)
    {
        Assert.notNull(failedNodeId);
        Assert.notNull(retransmittedMessages);
        
        this.failedNodeId = failedNodeId;
        this.flushId = flushId;
        this.retransmittedMessages = Immutables.wrap(retransmittedMessages);
        
        int size = 28;
        for (IMessage message : retransmittedMessages)
            size += message.getSize();
        
        this.size = size;
    }

    public UUID getFailedNodeId()
    {
        return failedNodeId;
    }
    
    public long getFlushId()
    {
        return flushId;
    }
    
    public List<IMessage> getRetransmittedMessages()
    {
        return retransmittedMessages;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }
    
    @Override 
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(messages.partHeader(failedNodeId, flushId).toString());
        
        for (IMessage message : retransmittedMessages)
        {
            builder.append('\n');
            builder.append(Strings.wrap(message.toString(), 4, 120));
        }
        
        return builder.toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("failed node-id: {0}, flush-id: {1}, messages:")
        ILocalizedMessage partHeader(UUID failedNodeId, long flushId);
    }
}

