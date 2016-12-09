/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;





/**
 * The {@link ICompartmentGroupMXBean} is a management interface of compartment group. 
 * to this group.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentGroupMXBean
{
    /** Name of MXBean of compartment group.*/
    final String MBEAN_NAME = "com.exametrika.common:type=CompartmentGroup";
    
    /**
     * Returns name of the group.
     *
     * @return name of the group
     */
    String getName();
    
    /**
     * Returns timer period in milliseconds.
     *
     * @return timer period in milliseconds
     */
    long getTimerPeriod();
    
    /**
     * Sets timer period in milliseconds.
     *
     * @param period timer period in milliseconds
     */
    void setTimerPeriod(long period);
    
    /**
     * Returns pool thread count.
     *
     * @return pool thread count
     */
    int getThreadCount();
    
    /**
     * Sets pool thread count.
     *
     * @param count pool thread count
     */
    void setThreadCount(int count);
}
