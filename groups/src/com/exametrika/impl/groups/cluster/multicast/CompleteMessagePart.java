/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link CompleteMessagePart} is a protocol completion message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompleteMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long completedMessageId;

    public CompleteMessagePart(long completedMessageId)
    {
        this.completedMessageId = completedMessageId;
    }
    
    public long getCompletedMessageId()
    {
        return completedMessageId;
    }
    
    @Override
    public int getSize()
    {
        return 8;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(completedMessageId).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("completed message-id: {0}")
        ILocalizedMessage toString(long completedMessageId);
    }
}

