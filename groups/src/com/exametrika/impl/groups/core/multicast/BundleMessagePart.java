/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link BundleMessagePart} is a bundle message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class BundleMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long membershipId;
    private final long completedMessageId;
    private final ByteArray data;

    public BundleMessagePart(long membershipId, long completedMessageId, ByteArray data)
    {
        Assert.notNull(data);
        
        this.membershipId = membershipId;
        this.completedMessageId = completedMessageId;
        this.data = data;
    }
    
    public long getMembershipId()
    {
        return membershipId;
    }
    
    public long getCompletedMessageId()
    {
        return completedMessageId;
    }
    
    public ByteArray getData()
    {
        return data;
    }
    
    @Override
    public int getSize()
    {
        return 16 + data.getLength();
    }
    
    @Override 
    public String toString()
    {
       return messages.toString(membershipId, completedMessageId, data.getLength()).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("membership-id: {0}, completed message-id: {1}, data size: {2}")
        ILocalizedMessage toString(long membershipId, long completedMessageId, int dataSize);
    }
}

