/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.impl.ColorizingConsoleAppender;
import com.exametrika.common.log.impl.ConsoleAppender;
import com.exametrika.common.log.impl.IAppender;
import com.exametrika.common.log.impl.LogContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.OSes;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ConsoleAppenderConfiguration} is a configuration of console appender.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ConsoleAppenderConfiguration extends TextAppenderConfiguration
{
    private final Target target;
    private final boolean colorize;
    
    public enum Target
    {
        OUTPUT,
        
        ERROR
    }
    
    public ConsoleAppenderConfiguration(String name, LogLevel level, String template, List<String> filters,
        Target target, boolean colorize)
    {
        super(name, level, template, filters);
        
        Assert.notNull(target);
        
        this.target = target;
        this.colorize = colorize;
    }
    
    public Target getTarget()
    {
        return target;
    }

    public boolean isColorize()
    {
        return colorize;
    }
    
    @Override
    public IAppender createAppender(CompileContext compileContext, LogContext context, 
        Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
    {
        if (colorize && !OSes.IS_WINDOWS)
            return new ColorizingConsoleAppender(this, compileContext, context, runtimeContext, templateRegistry);
        else
            return new ConsoleAppender(this, compileContext, context, runtimeContext, templateRegistry);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof ConsoleAppenderConfiguration))
            return false;
        
        ConsoleAppenderConfiguration configuration = (ConsoleAppenderConfiguration)o;
        return super.equals(o) && target == configuration.target && colorize == configuration.colorize;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(target, colorize);
    }
}
