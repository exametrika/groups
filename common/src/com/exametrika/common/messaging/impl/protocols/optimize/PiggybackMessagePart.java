/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.List;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link PiggybackMessagePart} is a piggyback message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PiggybackMessagePart implements IMessagePart
{
    private final List<IMessagePart> parts;
    private final int size;

    public PiggybackMessagePart(List<IMessagePart> parts)
    {
        Assert.notNull(parts);
        
        this.parts = Immutables.wrap(parts);
        
        int size = 0;
        for (IMessagePart part : parts)
            size += part.getSize();
        
        this.size = size + 4;
    }
    
    public List<IMessagePart> getParts()
    {
        return parts;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }
    
    @Override 
    public String toString()
    {
       return parts.toString();
    }
}

