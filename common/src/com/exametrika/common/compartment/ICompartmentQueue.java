/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import com.exametrika.common.utils.Assert;







/**
 * The {@link ICompartmentQueue} is a compartment queue which allows to redefine queing logic of tasks in compartment.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentQueue
{
    /** Queue event. */
    class Event
    {
        public final Object task;
        public final int size;
        public final ICompartment compartment;
        
        public Event(Object task, ICompartment compartment, int size)
        {
            Assert.notNull(task);
            
            this.task = task;
            this.size = size;
            this.compartment = compartment;
        }
        
        @Override
        public String toString()
        {
            return task.toString();
        }
    }
    
    /**
     * Returns queue capacity.
     *
     * @return queue capacity
     */
    int getCapacity();
    
    /**
     * Offers new event to the queue.
     *
     * @param event event
     */
    void offer(Event event);
    
    /**
     * Polls event from the queue.
     *
     * @param firstInBatch true if poll request is first in current run of event processing
     * @return event or null if there is no events in current batch run (but queue may have events)
     */
    Event poll(boolean firstInBatch);
}
