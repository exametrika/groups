/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplate;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.expression.Templates;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;



/**
 * The {@link TextAppender} is an abstract text appender.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public abstract class TextAppender extends AbstractAppender
{
    private final ITemplate template;

    public TextAppender(LogLevel level, String template, List<String> filters, CompileContext compileContext, 
        LogContext context, Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
    {
        super(level, filters, compileContext, context, runtimeContext);
        
        Assert.notNull(templateRegistry);
        Assert.notNull(template);
        
        this.template = Templates.compile(template, compileContext, templateRegistry);
    }
    
    @Override
    protected void doAppend(LogEvent event)
    {
        context.setEvent(event);
        String message = template.execute(context, runtimeContext);
        context.setEvent(null);
        
        doAppend(message);
    }
    
    protected abstract void doAppend(String message);
}
