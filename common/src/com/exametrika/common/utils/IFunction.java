/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link IFunction} represents a some function to evaluate.
 * 
 * @param <IV> input value type of a function
 * @param <OV> output value type of a function
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IFunction<IV, OV>
{
    /**
     * Evaluates function for the specified value.
     * 
     * @param value input value of function
     * @return result of function evaluation
     */
    OV evaluate(IV value);
}
