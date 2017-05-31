/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

public final class TestLoadMessagePart implements IMessagePart
{
    private final int index;
    private final long count;
    private final ByteArray value;

    public TestLoadMessagePart(int index, long count, ByteArray value)
    {
        Assert.notNull(value);
        
        this.index = index;
        this.count = count;
        this.value = value;
    }
    
    public int getIndex()
    {
        return index;
    }
    
    public long getCount()
    {
        return count;
    }
    
    public ByteArray getValue()
    {
        return value;
    }
    
    @Override
    public int getSize()
    {
        return 17 + value.getLength();
    }
    
    @Override
    public String toString()
    {
        return Integer.toString(index) + ":" + Long.toString(count);
    }
}