/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;

/**
 * The {@link AbstractAddress} represents an abstract address implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractAddress implements IAddress
{
    private final UUID id;
    private final String name;

    public AbstractAddress(UUID id, String name)
    {
        Assert.notNull(id);
        Assert.notNull(name);

        this.id = id;
        this.name = name;
    }
    
    @Override
    public final UUID getId()
    {
        return id;
    }
    
    @Override
    public final String getName()
    {
        return name;
    }
    
    @Override
    public final String toString()
    {
        return getName();
    }
    
    @Override
    public final boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof AbstractAddress))
            return false;
        
        AbstractAddress address = (AbstractAddress)o;
        return id.equals(address.id);
    }
    
    @Override
    public final int hashCode()
    {
        return id.hashCode();
    }
    
    @Override
    public final int compareTo(IAddress o)
    {
        return id.compareTo(o.getId());
    }
}
