/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;


/**
 * The {@link ColorizingConsoleAppender} is a colorizing console appender.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class ColorizingConsoleAppender extends TextAppender
{
    private final boolean output;
    
    public ColorizingConsoleAppender(ConsoleAppenderConfiguration configuration, CompileContext compileContext, 
        LogContext context, Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
    {
        super(configuration.getLevel(), configuration.getTemplate(), configuration.getFilters(), compileContext, 
            context, runtimeContext, templateRegistry);
        
        output = configuration.getTarget() == Target.OUTPUT;
    }
    
    @Override
    public void flush()
    {
    }

    @Override
    public void start()
    {
    }
    
    @Override
    public void stop()
    {
    }

    @Override
    public String toString()
    {
        return super.toString() + " colorizing console";
    }
    
    @Override
    protected void doAppend(LogEvent event)
    {
        context.setColorize(true);
        super.doAppend(event);
        context.setColorize(false);
    }
    
    @Override
    protected void doAppend(String message)
    {
        if (output)
            System.out.println(message);
        else
            System.err.println(message);
    }
}
