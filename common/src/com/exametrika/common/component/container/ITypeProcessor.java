/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

/**
 * The {@link ITypeProcessor} is used to post process component types.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITypeProcessor
{
    /**
     * Processes a component type.
     *
     * @param componentType input component type
     * @return output component type
     */
    Object processType(Object componentType);
}
