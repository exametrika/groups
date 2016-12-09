/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;




/**
 * The {@link LogInterceptor} is a log interceptor.
 * 
 * @threadsafety Implementations of this class and its methods are thread safe.
 * @author AndreyM
 */
public class LogInterceptor
{
    public static LogInterceptor INSTANCE = new LogInterceptor();
    
    public void onLog(LogEvent event)
    {
    }
}
