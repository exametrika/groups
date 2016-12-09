/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentQueue;
import com.exametrika.common.utils.SimpleDeque;



/**
 * The {@link SimpleCompartmentQueue} is a simple compartment queue.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleCompartmentQueue implements ICompartmentQueue
{
    private final SimpleDeque<Event> queue = new SimpleDeque<Event>();
    private int capacity;
    
    @Override
    public int getCapacity()
    {
        return capacity;
    }
    
    @Override
    public void offer(Event event)
    {
        capacity += event.size;
        queue.offer(event);
    }

    @Override
    public Event poll(boolean firstInBatch)
    {
        Event event = queue.poll();
        if (event != null)
            capacity -= event.size;
        
        return event;
    }
}
