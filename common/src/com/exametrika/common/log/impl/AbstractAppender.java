/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;



/**
 * The {@link AbstractAppender} is an abstract appender.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public abstract class AbstractAppender implements IAppender
{
    private final LogLevel level;
    private final List<IExpression> filters;
    protected final LogContext context;
    protected final Map<String, Object> runtimeContext;

    public AbstractAppender(LogLevel level, List<String> filters, CompileContext compileContext, LogContext context,
        Map<String, Object> runtimeContext)
    {
        Assert.notNull(filters);
        Assert.notNull(compileContext);
        Assert.notNull(context);
        Assert.notNull(runtimeContext);
        
        this.level = level;
        
        List<IExpression> compiledFilters = new ArrayList<IExpression>();
        for (String filter : filters)
            compiledFilters.add(Expressions.compile(filter, compileContext));

        this.filters = compiledFilters;
        this.context = context;
        this.runtimeContext = runtimeContext;
    }
    
    @Override
    public final void append(LogEvent event)
    {
        Assert.notNull(event);
        
        if (this.level != null && event.getLevel().ordinal() < this.level.ordinal())
            return;
        
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
        
        doAppend(event);
    }

    @Override
    public String toString()
    {
        return "[" + level + "]";
    }
    
    protected abstract void doAppend(LogEvent event);
}
