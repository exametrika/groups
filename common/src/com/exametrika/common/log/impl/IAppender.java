/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import com.exametrika.common.utils.ILifecycle;




/**
 * The {@link IAppender} represents a logger appender.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IAppender extends ILifecycle
{
    /**
     * Appends log event to logging stream.
     *
     * @param event log event
     */
    void append(LogEvent event);

    /**
     * Flushes logging stream.
     */
    void flush();
}
