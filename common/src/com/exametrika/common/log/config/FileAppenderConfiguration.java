/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.impl.FileAppender;
import com.exametrika.common.log.impl.IAppender;
import com.exametrika.common.log.impl.LogContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link FileAppenderConfiguration} is a configuration of file appender.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FileAppenderConfiguration extends TextAppenderConfiguration
{
    private final File path;

    public FileAppenderConfiguration(String name, LogLevel level, String template, File path, List<String> filters)
    {
        super(name, level, template, filters);
        
        Assert.notNull(path);
        
        this.path = path;
    }
    
    public File getPath()
    {
        return path;
    }

    @Override
    public IAppender createAppender(CompileContext compileContext, LogContext context, 
        Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
    {
        return new FileAppender(this, compileContext, context, runtimeContext, templateRegistry);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof FileAppenderConfiguration))
            return false;
        
        FileAppenderConfiguration configuration = (FileAppenderConfiguration)o;
        return super.equals(o) && path.equals(configuration.path);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(path);
    }
}
