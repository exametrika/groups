/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

/**
 * The {@link IFlushCondition} is a flush starting condition.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlushCondition
{
    /**
     * Can flush be started now?
     *
     * @return true if flush can be started now
     */
    boolean canStartFlush();
}
