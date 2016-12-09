/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;




/**
 * The {@link ICompartmentGroupProcessor} is a compartment group processor which represents additional prcessing logic 
 * called from timer thread of compartment group.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentGroupProcessor
{
    /**
     * Called from timer thread of compartment group.
     *
     * @param currentTime current time
     */
    void onTimer(long currentTime);
}
