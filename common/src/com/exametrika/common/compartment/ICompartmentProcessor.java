/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;




/**
 * The {@link ICompartmentProcessor} is a compartment processor which represents additional prcessing logic called from
 * main compartment thread.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentProcessor
{
    /**
     * Called from main compartment thread.
     */
    void process();
}
