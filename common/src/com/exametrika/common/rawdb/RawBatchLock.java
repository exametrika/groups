/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.io.Serializable;
import java.util.List;

import com.exametrika.common.utils.Assert;





/**
 * The {@link RawBatchLock} is a batch lock.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawBatchLock implements Serializable
{
    private final Type type;
    private final String predicate;

    public enum Type
    {
        SHARED,
        EXCLUSIVE
    }
    
    public RawBatchLock(Type type, String predicate)
    {
        Assert.notNull(type);
        Assert.notNull(predicate);
        
        this.type = type;
        this.predicate = predicate;
    }
    
    public Type getType()
    {
        return type;
    }
    
    public String getPredicate()
    {
        return predicate;
    }
    
    public boolean allow(boolean readOnly, List<String> predicates)
    {
        if (readOnly && type == Type.SHARED)
            return true;
        if (predicates == null || predicates.isEmpty())
            return true;
        
        for (String predicate : predicates)
        {
            if (predicate.length() >= this.predicate.length() && predicate.startsWith(this.predicate))
                return false;
            else if (this.predicate.startsWith(predicate))
                return false;
        }
        
        return true;
    }
}
