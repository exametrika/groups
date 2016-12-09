/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

/**
 * The {@link IComponentProcessor} is used to post process component instances after their creation.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IComponentProcessor
{
    /**
     * Processes a component instance before its start.
     *
     * @param component input component instance
     * @return output component instance
     */
    Object processBeforeStart(Object component);
    
    /**
     * Processes a component instance after its start.
     *
     * @param component input component instance
     * @return output component instance
     */
    Object processAfterStart(Object component);
}
