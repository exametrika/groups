/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;



/**
 * The {@link IClassResolver} is a resolver of classes used by expressions.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IClassResolver
{
    /**
     * Resolves class by name.
     *
     * @param className class name
     * @return class instance
     */
    Class resolveClass(String className);
}
