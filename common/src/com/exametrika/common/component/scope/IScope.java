/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

/**
 * The {@link IScope} represents a component scope instance.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IScope
{
    /**
     * Returns component from scope.
     *
     * @param <T> component type
     * @param componentName name of component in scope
     * @return component or null if component is not found
     */
    <T> T get(String componentName);
    
    /**
     * Adds component to scope.
     *
     * @param componentName name of component in scope
     * @param component component
     * @exception ComponentAlreadyInScopeException if another component already exists in scope
     */
    void add(String componentName, Object component);
}
