/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;

import java.util.HashMap;
import java.util.Map;



/**
 * The {@link LoggingContext} is a logging context.
 *
 * @see ILogger
 * @see IMarker
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class LoggingContext
{
    private static final ThreadLocal<Map<String, Object>> context = new InheritableThreadLocal<Map<String, Object>>()
    {
        @Override
        protected Map<String, Object> initialValue() 
        {
            return new HashMap<String, Object>();
        }
        
        @Override
        protected Map<String, Object> childValue(Map<String, Object> parentValue) 
        {
            return new HashMap<String, Object>(parentValue);
        }
    };
    
    public static <T> T get(String key)
    {
        return (T)context.get().get(key);
    }
    
    public static <T> T put(String key, Object value)
    {
        return (T)context.get().put(key, value);
    }
    
    public static <T> T remove(String key)
    {
        return (T)context.get().remove(key);
    }
    
    public static void clear()
    {
        context.get().clear();
    }
    
    public static Map<String, Object> getContext()
    {
        return context.get();
    }
    
    private LoggingContext()
    {
    }
}
