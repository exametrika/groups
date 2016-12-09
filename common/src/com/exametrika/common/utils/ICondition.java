/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link ICondition} represents a some condition to evaluate.
 * 
 * @param <V> value type of a condition
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ICondition<V>
{
    /**
     * Evaluates condition for the specified value.
     * 
     * @param value value to evaluate condition for
     * @return result of condition evaluation
     */
    boolean evaluate(V value);
}
