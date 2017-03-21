/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link FailureAtomicMessagePart} is a failure atomic message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long messageId;
    private final long order;

    public FailureAtomicMessagePart(long messageId, long order)
    {
        this.messageId = messageId;
        this.order = order;
    }
    
    public long getMessageId()
    {
        return messageId;
    }
    
    public long getOrder()
    {
        return order;
    }
    
    @Override
    public int getSize()
    {
        return 16;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(messageId, order).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("message-id: {0}, order: {1}")
        ILocalizedMessage toString(long messageId, long order);
    }
}

