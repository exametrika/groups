/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;




/**
 * The {@link ILoadContext} represents configuration load context.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILoadContext
{
    /**
     * Finds initialization parameter.
     *
     * @param name parameter name
     * @return initialization parameter or null if parameter with specified name is not found
     */
    <T> T findParameter(String name);
    
    /**
     * Sets initialization parameter.
     *
     * @param name parameter name
     * @param value parameter value
     */
    void setParameter(String name, Object value);
    
    /**
     * Returns load context of specified configuration type.
     *
     * @param configurationType type of configuration
     * @return load context or null if load context for specified configuration type is not found
     */
    <T> T get(String configurationType);
}
