/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import com.exametrika.common.log.config.LoggingConfiguration;





/**
 * The {@link ILoggingService} is a logging service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ILoggingService extends ILoggerFactory
{
    String NAME = "logging";
    
    /**
     * Returns current logging configuration.
     *
     * @return current logging configuration
     */
    LoggingConfiguration getConfiguration();
    
    /**
     * Updates logging configuration in all loggers, registered in this manager.
     *
     * @param configuration configuration to update
     */
    void setConfiguration(LoggingConfiguration configuration);
}
