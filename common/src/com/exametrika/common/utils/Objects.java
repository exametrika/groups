/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.Arrays;


/**
 * The {@link Objects} contains different utility methods for object manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Objects
{
    /**
     * Checks equality of specified values.
     *
     * @param value1 first value. Can be null
     * @param value2 second value. Can be null
     * @return true if values equal
     */
    public static boolean equals(Object value1, Object value2)
    {
        if (value1 != value2 && (value1 == null || !value1.equals(value2)))
            return false;
        
        return true;
    }
    
    /**
     * Returns combined hash code of specified arguments.
     *
     * @param args arguments whose combined hash code must be calculated
     * @return arguments combined hash code
     */
    public static final int hashCode(Object ... args)
    {
        return Arrays.hashCode(args);
    }
    
    private Objects()
    {
    }
}
