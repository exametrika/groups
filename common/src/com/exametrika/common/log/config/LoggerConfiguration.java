/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.util.Collections;
import java.util.List;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link LoggerConfiguration} is a configuration of logger.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LoggerConfiguration extends Configuration
{
    private final String name;
    private final LogLevel level;
    private final List<String> appenders;
    private final List<String> filters;
    private final boolean recordStackTrace;

    public LoggerConfiguration()
    {
        this("", LogLevel.ERROR, Collections.<String>emptyList(), Collections.<String>emptyList(), false);
    }
    
    public LoggerConfiguration(String name, LogLevel level, List<String> appenders, List<String> filters,
        boolean recordStackTrace)
    {
        Assert.notNull(name);
        Assert.notNull(level);
        Assert.notNull(appenders);
        Assert.notNull(filters);
        
        this.name = name;
        this.level = level;
        this.appenders = Immutables.wrap(appenders);
        this.filters = Immutables.wrap(filters);
        this.recordStackTrace = recordStackTrace;
    }
    
    public String getName()
    {
        return name;
    }

    public LogLevel getLevel()
    {
        return level;
    }

    public List<String> getAppenders()
    {
        return appenders;
    }
    
    public List<String> getFilters()
    {
        return filters;
    }

    public boolean isRecordStackTrace()
    {
        return recordStackTrace;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof LoggerConfiguration))
            return false;
        
        LoggerConfiguration configuration = (LoggerConfiguration)o;
        return name.equals(configuration.name) && level.equals(configuration.level) && 
            appenders.equals(configuration.appenders) && filters.equals(configuration.filters) &&
            recordStackTrace == configuration.recordStackTrace;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, level, appenders, filters, recordStackTrace);
    }
}
