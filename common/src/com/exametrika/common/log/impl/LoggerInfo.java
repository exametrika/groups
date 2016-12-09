/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.IExpression;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;



/**
 * The {@link LoggerInfo} is an information about logger.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class LoggerInfo
{
    private final String name;
    private final LogLevel level;
    private final List<IAppender> appenders;
    private final List<IExpression> filters;
    private final boolean recordStackTrace;
    private final LogContext context;
    private final Map<String, Object> runtimeContext;
    
    public LoggerInfo(String name, LogLevel level, List<? extends IAppender> appenders, List<IExpression> filters,
        boolean recordStackTrace, LogContext context, Map<String, Object> runtimeContext)
    {
        Assert.notNull(name);
        Assert.notNull(level);
        Assert.notNull(appenders);
        Assert.notNull(filters);
        Assert.notNull(context);
        Assert.notNull(runtimeContext);
        
        this.name = name;
        this.level = level;
        this.appenders = Immutables.wrap(appenders);
        this.filters = Immutables.wrap(filters);
        this.recordStackTrace = recordStackTrace;
        this.context = context;
        this.runtimeContext = runtimeContext;
    }
    
    public String getName()
    {
        return name;
    }
    
    public LogLevel getLevel()
    {
        return level;
    }
    
    public List<IAppender> getAppenders()
    {
        return appenders;
    }
    
    public List<IExpression> getFilters()
    {
        return filters;
    }

    public boolean isRecordStackTrace()
    {
        return recordStackTrace;
    }
    
    public void append(LogEvent event)
    {
        if (!filters.isEmpty())
        {
            context.setEvent(event);
            
            boolean allow = false;
            for (IExpression filter : filters)
            {
                if (filter.execute(context, runtimeContext))
                {
                    allow = true;
                    break;
                }
            }
            
            context.setEvent(null);
            
            if (!allow)
                return;
        }
        
        for (IAppender appender : appenders)
            appender.append(event);
    }
    
    @Override
    public String toString()
    {
        return "[" + level + "] " + name + " " + appenders;
    }
}
