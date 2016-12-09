/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;





/**
 * The {@link ICacheable} represents an interface to get cache size of object.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author AndreyM
 */
public interface ICacheable
{
    /**
     * Returns cache size of object.
     *
     * @return cache size of object
     */
    int getCacheSize();
}
