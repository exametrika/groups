/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

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
    private final ByteArray data;

    public BundleMessagePart(ByteArray data)
    {
        Assert.notNull(data);
        
        this.data = data;
    }
    
    public ByteArray getData()
    {
        return data;
    }
    
    @Override
    public int getSize()
    {
        return data.getLength();
    }
    
    @Override 
    public String toString()
    {
       return messages.toString(data.getLength()).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("data size: {0}")
        ILocalizedMessage toString(int dataSize);
    }
}

