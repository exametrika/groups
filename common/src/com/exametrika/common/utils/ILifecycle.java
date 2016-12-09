/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link ILifecycle} represents a component that has lifecycle.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILifecycle
{
    /**
     * Starts a component.
     * @exception InvalidStateException if already started
     */
    void start();
    
    /**
     * Stops a component.
     */
    void stop();
}
