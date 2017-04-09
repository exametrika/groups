/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link SimpleStateTransferResponseMessagePart} is a simple state transfer response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleStateTransferResponseMessagePart implements IMessagePart
{
    private final ByteArray state;

    public SimpleStateTransferResponseMessagePart(ByteArray state)
    {
        Assert.notNull(state);
        
        this.state = state;
    }
    
    public ByteArray getState()
    {
        return state;
    }
   
    @Override
    public int getSize()
    {
        return state.getLength();
    }
    
    @Override 
    public String toString()
    {
        return Integer.toString(state.getLength());
    }
}

