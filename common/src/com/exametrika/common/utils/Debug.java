/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;

import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;

/**
 * The {@link Debug} contains various debug functions.
 * 
 * @author Medvedev-A
 */
public final class Debug
{
    private static final boolean DEBUG;
    private static final boolean PROFILE;
    public static final ILogger logger = Loggers.get(Debug.class);
    
    static
    {
        String args = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        DEBUG = args.contains("-agentlib:jdwp") || args.contains("-Xrunjdwp");
        PROFILE = System.getenv("EXA_PROFILE") != null;
    }
    
    public static boolean isDebug()
    {
        return DEBUG;
    }

    public static boolean isProfile()
    {
        return PROFILE;
    }

    public static void log(LogLevel level, String message, Object... args)
    {
        logger.log(level, new NonLocalizedMessage(MessageFormat.format(message, args)));
    }
    
    public static void log(LogLevel level, Throwable exception)
    {
        logger.log(level, exception);
    }
    
    public static void logError()
    {
        log(LogLevel.ERROR, new Exception());
    }
    
    public static void print(String message)
    {
        System.out.println(message);
    }
    
    public static void print(String message, Object... args)
    {
        print(MessageFormat.format(message, args));
    }

    public static void printStackTrace(String message)
    {
        printStackTrace(Thread.currentThread(), message);
    }

    public static void printStackTrace(Thread thread, String message)
    {
        print(StackTraces.getStackTrace(thread, message));
    }

    public static void printAllStackTraces()
    {
        print("==============================\n" + StackTraces.getAllStackTraces());
    }
    
    public static void startDumpThreads(final long delay, final long period)
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Threads.sleep(delay);
                
                while (true)
                {
                    printAllStackTraces();
                    Threads.sleep(period);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private Debug()
    {
    }
}
