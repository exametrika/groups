/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Closeable;





/**
 * The {@link HolderManager} is manager of {@link Holder}.
 * 
 * @param <T> resource type 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class HolderManager<T extends Closeable>
{
    private final SimpleList<Holder<T>> holders = new SimpleList<Holder<T>>();
    
    public synchronized Holder<T> createHolder(T instance)
    {
        Holder<T> holder = new Holder<T>(instance, this);
        holders.addFirst(holder.getElement());
        
        return holder;
    }
    
    public synchronized void close()
    {
        for (Holder<T> holder : holders.values())
            holder.close();
        
        Assert.checkState(holders.isEmpty());
    }
}
