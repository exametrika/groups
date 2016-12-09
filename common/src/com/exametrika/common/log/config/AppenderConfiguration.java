/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.util.List;
import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.impl.IAppender;
import com.exametrika.common.log.impl.LogContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AppenderConfiguration} is a configuration of base abstract appender.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AppenderConfiguration extends Configuration
{
    protected final String name;
    protected final LogLevel level;
    protected final List<String> filters;

    public AppenderConfiguration(String name, LogLevel level, List<String> filters)
    {
        Assert.notNull(name);
        Assert.notNull(filters);
        
        this.name = name;
        this.level = level;
        this.filters = filters;
    }
    
    public final String getName()
    {
        return name;
    }

    public final LogLevel getLevel()
    {
        return level;
    }

    public final List<String> getFilters()
    {
        return filters;
    }
    
    public abstract IAppender createAppender(CompileContext compileContext, LogContext context, 
        Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry);
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof AppenderConfiguration))
            return false;
        
        AppenderConfiguration configuration = (AppenderConfiguration)o;
        return name.equals(configuration.name) && Objects.equals(level, configuration.level) && 
            filters.equals(configuration.filters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, level, filters);
    }
}
