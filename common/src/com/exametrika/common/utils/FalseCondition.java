/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

/**
 * The {@link FalseCondition} is false condition.
 * 
 * @param <T> condition type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FalseCondition<T> implements ICondition<T>
{
    @Override
    public boolean evaluate(T value)
    {
        return false;
    }
}
