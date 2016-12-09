/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;



/**
 * The {@link ILoggerFactory} is a logger factory.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILoggerFactory
{
    /**
     * Creates a logger.
     *
     * @param name logger name
     * @return logger
     */
    ILogger createLogger(String name);
    
    /**
     * Creates a marker.
     *
     * @param name marker name
     * @param references marker references. Can be null
     * @return marker
     */
    IMarker createMarker(String name, IMarker[] references);
}
