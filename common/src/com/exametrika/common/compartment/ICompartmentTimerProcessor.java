/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;




/**
 * The {@link ICompartmentTimerProcessor} is a compartment processor which represents additional prcessing logic called from
 * main compartment thread by timer.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentTimerProcessor
{
    /**
     * Called from main compartment thread by timer.
     *
     * @param currentTime current time
     */
    void onTimer(long currentTime);
}
