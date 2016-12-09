/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;

import com.exametrika.common.utils.Assert;

/**
 * The {@link LogLevel} defines logging levels for {@link ILogger}.
 * 
 * @author Medvedev-A
 */
public enum LogLevel
{
    /** All level. */
    ALL,
    
    /** Trace level. */
    TRACE,
    
    /** Debug level. */
    DEBUG,
    
    /** Info level. */
    INFO,
    
    /** Warning level. */
    WARNING,
    
    /** Error level. */
    ERROR,
    
    /** Off level. */
    OFF;
    
    public String toJson()
    {
        switch (this)
        {
        case ALL:
            return "all";
        case TRACE:
            return "trace";
        case DEBUG:
            return "debug";
        case INFO:
            return "info";
        case WARNING:
            return "warning";
        case ERROR:
            return "error";
        case OFF:
            return "off";
        default:
            return Assert.error();
        }
    }
}
