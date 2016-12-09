/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;

import com.exametrika.common.l10n.ILocalizedMessage;



/**
 * The {@link ILogger} represents a logger.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILogger
{
    /**
     * Returns logger name.
     *
     * @return logger name
     */
    String getName();
    
    /**
     * If specified level logs are enabled?
     *
     * @param level log level
     * @return true if specified level logs are enabled
     */
    boolean isLogEnabled(LogLevel level);
    
    /**
     * Logs message at specified level.
     *
     * @param level log level
     * @param message message
     */
    void log(LogLevel level, ILocalizedMessage message);
    
    /**
     * Logs message and exception at specified level.
     *
     * @param level log level
     * @param message message
     * @param exception exception to log
     */
    void log(LogLevel level, ILocalizedMessage message, Throwable exception);
    
    /**
     * Logs exception at specified level.
     * 
     * @param level log level
     * @param exception exception to log
     */
    void log(LogLevel level, Throwable exception);
    
    /**
     * If specified level logs are enabled?
     *
     * @param level log level
     * @param marker marker. Can be null
     * @return true if specified level logs are enabled
     */
    boolean isLogEnabled(LogLevel level, IMarker marker);

    /**
     * Logs message at specified level.
     *
     * @param level log level
     * @param marker marker. Can be null
     * @param message message
     */
    void log(LogLevel level, IMarker marker, ILocalizedMessage message);
    
    /**
     * Logs message and exception at specified level.
     *
     * @param level log level
     * @param marker marker. Can be null
     * @param message message
     * @param exception exception to log
     */
    void log(LogLevel level, IMarker marker, ILocalizedMessage message, Throwable exception);
    
    /**
     * Logs exception at specified level.
     *
     * @param level log level
     * @param marker marker. Can be null
     * @param exception exception to log
     */
    void log(LogLevel level, IMarker marker, Throwable exception);
}
