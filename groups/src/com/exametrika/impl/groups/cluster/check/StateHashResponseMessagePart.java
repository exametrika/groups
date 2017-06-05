/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link StateHashResponseMessagePart} is a group state hash response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateHashResponseMessagePart implements IMessagePart
{
    private final String hash;

    public StateHashResponseMessagePart(String hash)
    {
        Assert.notNull(hash);
        
        this.hash = hash;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    @Override
    public int getSize()
    {
        return hash.length() * 2;
    }
    
    @Override 
    public String toString()
    {
        return hash;
    }
}

