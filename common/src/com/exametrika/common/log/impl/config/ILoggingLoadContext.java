/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl.config;

import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;




/**
 * The {@link ILoggingLoadContext} represents a logging configuration load context.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILoggingLoadContext
{
    /**
     * Adds appender.
     *
     * @param appender appender configuration
     */
    void addAppender(AppenderConfiguration appender);
    
    /**
     * Adds logger.
     *
     * @param logger logger configuration
     */
    void addLogger(LoggerConfiguration logger);
}
