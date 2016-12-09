/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link TestMode} contains a switcher of global test mode.
 * 
 * @author Medvedev-A
 */
public final class TestMode
{
    private static boolean test;
    
    public static boolean isTest()
    {
        return test;
    }
    
    public static void setTest(boolean value)
    {
        test = value;
    }
    
    private TestMode()
    {
    }
}
