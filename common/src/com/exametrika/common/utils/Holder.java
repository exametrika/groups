/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import com.exametrika.common.utils.SimpleList.Element;





/**
 * The {@link Holder} is ref-countable resource holder.
 * 
 * @param <T> resource type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Holder<T extends Closeable>
{
    private final Object sync;
    private final AtomicInteger refCount = new AtomicInteger(1);
    private final Element<Holder<T>> element = new Element<Holder<T>>(this);
    private volatile T instance;
    
    public Holder(T instance, Object sync)
    {
        Assert.notNull(instance);
        Assert.notNull(sync);
        
        this.instance = instance;
        this.sync = sync;
    }

    Element<Holder<T>> getElement()
    {
        return element;
    }
    
    public T get()
    {
        T instance = this.instance;
        Assert.checkState(instance != null);
        return instance;
    }
    
    public boolean canClose()
    {
        return refCount.get() <= 1;
    }
    
    public void addRef()
    {
        refCount.incrementAndGet();
    }
    
    public void release()
    {
        if (refCount.decrementAndGet() == 0)
            close();
    }
    
    public void close()
    {
        synchronized (sync)
        {
            if (instance != null)
            {
                IOs.close(instance);
                instance = null;
                element.remove();
            }
        }
    }
}
