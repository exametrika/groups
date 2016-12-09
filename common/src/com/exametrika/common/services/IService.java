/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;

import com.exametrika.common.config.ILoadContext;




/**
 * The {@link IService} represents a service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IService
{
    /**
     * Wires service dependencies.
     *
     * @param registry service registry
     */
    void wire(IServiceRegistry registry);
    
    /**
     * Starts service.
     *
     * @param registry service registry
     */
    void start(IServiceRegistry registry);
    
    /**
     * Stops service.
     * 
     * @param fromShutdownHook if true service is stopped from shutdown hook
     */
    void stop(boolean fromShutdownHook);
    
    /**
     * Sets configuration of service from specified configuration load context.
     *
     * @param context configuration load context
     */
    void setConfiguration(ILoadContext context);
    
    /**
     * Called when service timer is elapsed.
     * 
     * @param currentTime currentTime
     */
    void onTimer(long currentTime);
}
