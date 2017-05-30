/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link StateChecksumResponseMessagePart} is a group state checksum response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateChecksumResponseMessagePart implements IMessagePart
{
    private final long checksum;

    public StateChecksumResponseMessagePart(long checksum)
    {
        Assert.notNull(checksum);
        
        this.checksum = checksum;
    }
    
    public long getChecksum()
    {
        return checksum;
    }
    
    @Override
    public int getSize()
    {
        return 8;
    }
    
    @Override 
    public String toString()
    {
        return Long.toString(checksum);
    }
}

