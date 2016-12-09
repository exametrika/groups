/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.config.FileAppenderConfiguration;
import com.exametrika.common.utils.IOs;



/**
 * The {@link FileAppender} is a file appender.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class FileAppender extends TextAppender
{
    private final File fileName;
    private Writer writer;
    private boolean changed;

    public FileAppender(FileAppenderConfiguration configuration, CompileContext compileContext, 
        LogContext context, Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
    {
        super(configuration.getLevel(), configuration.getTemplate(), configuration.getFilters(), compileContext, 
            context, runtimeContext, templateRegistry);
        
        this.fileName = configuration.getPath();
    }
    
    @Override
    public synchronized void flush()
    {
        if (!changed)
            return;
        
        try
        {
            writer.flush();
            changed = false;
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public synchronized void start()
    {
        try
        {
            fileName.getParentFile().mkdirs();
            this.writer = new BufferedWriter(new FileWriter(fileName, true));
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    @Override
    public synchronized void stop()
    {
        IOs.close(writer);
        writer = null;
    }
    
    @Override
    public String toString()
    {
        return super.toString() + " file:" + fileName;
    }
    
    @Override
    protected synchronized void doAppend(String message)
    {
        if (writer == null)
            return;
        
        try
        {
            writer.write(message);
            writer.write('\n');
            changed = true;
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
    }
}
