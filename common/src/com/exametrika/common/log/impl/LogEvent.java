/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;

/**
 * The {@link LogEvent} represents a single logging event.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class LogEvent implements Runnable
{
    private final LoggerInfo loggerInfo;
    private final LogLevel level;
    private final IMarker marker;
    private final ILocalizedMessage message;
    private final Throwable exception;
    private final String thread;
    private final StackTraceElement[] stackTrace;
    private final long time;

    public LogEvent(LoggerInfo loggerInfo, LogLevel level, IMarker marker, ILocalizedMessage message, Throwable exception,
        String thread, StackTraceElement[] stackTrace, long time)
    {
        this.loggerInfo = loggerInfo;
        this.level = level;
        this.marker = marker;
        this.message = message;
        this.exception = exception;
        this.thread = thread;
        this.stackTrace = stackTrace;
        this.time = time;
    }
    
    public String getLogger()
    {
        return loggerInfo.getName();
    }
    
    public LogLevel getLevel()
    {
        return level;
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public ILocalizedMessage getMessage()
    {
        return message;
    }
    
    public Throwable getException()
    {
        return exception;
    }
    
    public String getThread()
    {
        return thread;
    }
    
    public StackTraceElement[] getStackTrace()
    {
        return stackTrace;
    }
    
    public long getTime()
    {
        return time;
    }
    
    @Override
    public void run()
    {
        loggerInfo.append(this);
    }
}