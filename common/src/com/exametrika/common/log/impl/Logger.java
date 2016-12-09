/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;



/**
 * The {@link Logger} is a simple {@link ILogger} implementation.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Logger implements ILogger
{
    private final LoggingService loggingService;
    private volatile LoggerInfo loggerInfo;

    public Logger(LoggingService loggingService, LoggerInfo loggerInfo)
    {
        Assert.notNull(loggingService);
        
        this.loggingService = loggingService;
        
        setLoggerInfo(loggerInfo);
    }
    
    public void setLoggerInfo(LoggerInfo loggerInfo)
    {
        Assert.notNull(loggerInfo);
        
        this.loggerInfo = loggerInfo;
    }
    
    @Override
    public String getName()
    {
        return loggerInfo.getName();
    }
    
    @Override
    public boolean isLogEnabled(LogLevel level)
    {
        Assert.notNull(level);
        
        return level.ordinal() >= loggerInfo.getLevel().ordinal();
    }
    
    @Override
    public void log(LogLevel level, ILocalizedMessage message)
    {
        Assert.notNull(level);
        Assert.notNull(message);
        
        doLog(level, null, message, null);
    }
    
    @Override
    public void log(LogLevel level, ILocalizedMessage message, Throwable exception)
    {
        Assert.notNull(level);
        
        doLog(level, null, message, exception);
    }
    
    @Override
    public void log(LogLevel level, Throwable exception)
    {
        Assert.notNull(level);
        Assert.notNull(exception);
        
        doLog(level, null, null, exception);
    }

    @Override
    public boolean isLogEnabled(LogLevel level, IMarker marker)
    {
        Assert.notNull(level);

        return isLogEnabled(level);
    }
    
    @Override
    public void log(LogLevel level, IMarker marker, ILocalizedMessage message)
    {
        Assert.notNull(level);
        Assert.notNull(message);
        
        doLog(level, marker, message, null);
    }
    
    @Override
    public void log(LogLevel level, IMarker marker, ILocalizedMessage message, Throwable exception)
    {
        Assert.notNull(level);
        Assert.notNull(message);
        Assert.notNull(exception);
        
        doLog(level, marker, message, exception);
    }
    
    @Override
    public void log(LogLevel level, IMarker marker, Throwable exception)
    {
        Assert.notNull(level);
        Assert.notNull(exception);
        
        doLog(level, marker, null, exception);
    }
    
    @Override
    public String toString()
    {
        return loggerInfo.toString();
    }
    
    private void doLog(LogLevel level, IMarker marker, ILocalizedMessage message, Throwable exception)
    {
        LoggerInfo loggerInfo = this.loggerInfo;
        
        if (level.ordinal() < loggerInfo.getLevel().ordinal())
            return;
        
        Thread currentThread = Thread.currentThread();
        StackTraceElement[] stackTrace = null;
        if (loggerInfo.isRecordStackTrace())
            stackTrace = currentThread.getStackTrace();
        
        LogEvent event = new LogEvent(loggerInfo, level, marker, message, exception, currentThread.getName(),
            stackTrace, Times.getCurrentTime());
        loggingService.addLog(event);
    }
}
