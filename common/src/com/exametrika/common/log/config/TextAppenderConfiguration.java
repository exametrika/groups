/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.util.List;

import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TextAppenderConfiguration} is a configuration of text appender.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class TextAppenderConfiguration extends AppenderConfiguration
{
    protected final String template;

    public TextAppenderConfiguration(String name, LogLevel level, String template, List<String> filters)
    {
        super(name, level, filters);
        
        Assert.notNull(template);
        
        this.template = template;
    }
    
    public final String getTemplate()
    {
        return template;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof TextAppenderConfiguration))
            return false;
        
        TextAppenderConfiguration configuration = (TextAppenderConfiguration)o;
        return super.equals(configuration) && template.equals(configuration.template);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + template.hashCode();
    }
}
