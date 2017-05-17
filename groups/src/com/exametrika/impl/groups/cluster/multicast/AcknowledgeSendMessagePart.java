/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link AcknowledgeSendMessagePart} is a receive acknowledge message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AcknowledgeSendMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long lastReceivedMessageId;
    private final boolean standalone;

    public AcknowledgeSendMessagePart(long lastReceivedMessageId, boolean standalone)
    {
        this.lastReceivedMessageId = lastReceivedMessageId;
        this.standalone = standalone;
    }
    
    public long getLastReceivedMessageId()
    {
        return lastReceivedMessageId;
    }
    
    public boolean isStandalone()
    {
        return standalone;
    }
    
    @Override
    public int getSize()
    {
        return 9;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(lastReceivedMessageId).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("last received message-id: {0}")
        ILocalizedMessage toString(long lastReceivedMessageId);
    }
}

