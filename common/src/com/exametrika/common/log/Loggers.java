/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;

import com.exametrika.common.log.impl.ILoggerFactory;
import com.exametrika.common.log.impl.LoggingService;



/**
 * The {@link Loggers} is a factory for {@link ILogger} and {@link IMarker} implementations.
 *
 * @see ILogger
 * @see IMarker
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Loggers
{
    /**
     * Returns current logger factory.
     *
     * @return current logger factory
     */
    public static ILoggerFactory getLoggerFactory()
    {
        return LoggingService.instance;
    }
    
    /**
     * Creates logger for specified class.
     *
     * @param clazz logged class
     * @return logger
     */
    public static ILogger get(Class<?> clazz)
    {
        return get(clazz.getName());
    }
    
    /**
     * Creates logger for specified name.
     *
     * @param name logger name
     * @return logger
     */
    public static ILogger get(final String name)
    {
        return LoggingService.instance.createLogger(name);
    }

    /**
     * Returns marker for specified name.
     *
     * @param name marker name
     * @param references marker references
     * @return marker
     */
    public static IMarker getMarker(String name, IMarker ... references)
    {
        return LoggingService.instance.createMarker(name, references);
    }
    
    private Loggers()
    {
    }
}
