/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import java.io.Closeable;




/**
 * The {@link ICompartmentDispatcher} is a compartment dispatcher which used to execute some blocking functionality in
 * main compartment thread (i.e. network selection, etc.).
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentDispatcher extends Closeable
{
    /**
     * Sets compartment this dispatcher belongs to.
     *
     * @param compartment compartment
     */
    void setCompartment(ICompartment compartment);
    
    /**
     * Blocks main compartment thread on specified period of time. Call to this method can return sooner if some external event
     * is occured.
     *
     * @param period blocking period of time in milliseconds. 0 mean zero timeout (without wait)
     */
    void block(long period);
    
    /**
     * Wakes up dispatcher unblocking main compartment thread.
     */
    void wakeup();

    /**
     * Can execution of main compartment thread be finished. Called from main compartment thread.
     *
     * @param stopRequested true if dispatcher stop has been requested
     * @return true if execution of main compartment thread can be finished
     */
    boolean canFinish(boolean stopRequested);

    /**
     * Performs cleanup before compartment threads are stopped.
     */
    public void beforeClose();
    
    /**
     * Performs cleanup after compartment threads are stopped.
     */
    @Override
    public void close();
}
