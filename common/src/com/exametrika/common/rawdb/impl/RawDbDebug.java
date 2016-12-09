/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;




/**
 * The {@link RawDbDebug} is a helper object allows page access debugging.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawDbDebug
{
    public static final boolean DEBUG = System.getProperty("com.exametrika.common.db.debug", "false").equals("true");
    public static volatile IHandler handler;
    
    public interface IHandler
    {
        void debug(int fileIndex, long pageIndex, int offset, int length);
    }
    
    public static void debug(int fileIndex, long pageIndex, int offset, int length)
    {
        IHandler handler = RawDbDebug.handler;
        if (handler != null)
            handler.debug(fileIndex, pageIndex, offset, length);
    }
    
    private RawDbDebug()
    {
    }
}
