/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MissingMessageInfo} is a exchange data containing information about missing messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MissingMessageInfo
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID failedSenderId;
    private final long lastReceivedMessageId;

    public MissingMessageInfo(UUID failedSenderId, long lastReceivedMessageId)
    {
        Assert.notNull(failedSenderId);
        
        this.failedSenderId = failedSenderId;
        this.lastReceivedMessageId = lastReceivedMessageId;
    }
    
    public UUID getFailedSenderId()
    {
        return failedSenderId;
    }

    public long getLastReceivedMessageId()
    {
        return lastReceivedMessageId;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(failedSenderId, lastReceivedMessageId).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("failed sender-id: {0}, last received message-id: {1}")
        ILocalizedMessage toString(UUID failedSenderId, long lastReceivedMessageId);
    }
}

