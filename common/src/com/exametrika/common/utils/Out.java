/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;



/**
 * The {@link Out} represents an output parameter.
 * 
 * @param <T> parameter type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Out<T>
{
    public T value;
    
    public Out()
    {
    }
    
    public Out(T value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return value != null ? value.toString() : "";
    }
}
