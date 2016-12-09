/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentDispatcher;
import com.exametrika.common.tasks.ThreadInterruptedException;



/**
 * The {@link SimpleCompartmentDispatcher} is a simple compartment dispatcher.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleCompartmentDispatcher implements ICompartmentDispatcher
{
    @Override
    public void setCompartment(ICompartment compartment)
    {
    }

    @Override
    public synchronized void block(long period)
    {
        if (period == 0)
            return;
        
        try
        {
            wait(period);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
    }

    @Override
    public synchronized void wakeup()
    {
        notify();
    }

    @Override
    public boolean canFinish(boolean stopRequested)
    {
        return stopRequested;
    }

    @Override
    public void beforeClose()
    {
    }

    @Override
    public void close()
    {
    }
}
