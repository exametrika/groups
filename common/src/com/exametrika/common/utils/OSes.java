/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link OSes} contains different utility methods for work with Operating Systems.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class OSes
{
    public static final boolean IS_WINDOWS = isWindows();
    public static final boolean IS_64_BIT = is64bit();
    
    private static boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    
    private static boolean is64bit()
    {
        String value = System.getProperty("sun.arch.data.model");
        if (value != null)
            return value.contains("64");
        else
            return System.getProperty("os.arch").contains("64");
    }
    
    private OSes()
    {
    }
}
