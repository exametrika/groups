/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Arrays;

/**
 * The {@link StackTraces} contains various debug functions.
 * 
 * @author Medvedev-A
 */
public final class StackTraces
{
    public static String getStackTrace(Thread thread, String message)
    {
        return thread.getName() + ":" + message + "\n" + Strings.toString(Arrays.asList(thread.getStackTrace()), true);
    }

    public static String getAllStackTraces()
    {
        ThreadInfo[] infos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < infos.length; i++)
            builder.append(toString(infos[i], 100));
        
        return builder.toString();
    }
    
    private static String toString(ThreadInfo info, int maxFrames)
    {
        StringBuilder builder = new StringBuilder("\"" + info.getThreadName() + "\"" + " Id=" + info.getThreadId() + 
            " " + info.getThreadState());
        
        if (info.getLockName() != null)
            builder.append(" on " + info.getLockName());

        if (info.getLockOwnerName() != null)
            builder.append(" owned by \"" + info.getLockOwnerName() + "\" Id=" + info.getLockOwnerId());

        if (info.isSuspended())
            builder.append(" (suspended)");

        if (info.isInNative())
            builder.append(" (in native)");

        builder.append('\n');
        
        int i = 0;
        for (; i < info.getStackTrace().length && i < maxFrames; i++)
        {
            StackTraceElement trace = info.getStackTrace()[i];
            builder.append("\tat " + trace.toString());
            builder.append('\n');
            if (i == 0 && info.getLockInfo() != null)
            {
                Thread.State state = info.getThreadState();
                switch (state)
                {
                case BLOCKED:
                    builder.append("\t-  blocked on " + info.getLockInfo());
                    builder.append('\n');
                    break;
                case WAITING:
                    builder.append("\t-  waiting on " + info.getLockInfo());
                    builder.append('\n');
                    break;
                case TIMED_WAITING:
                    builder.append("\t-  waiting on " + info.getLockInfo());
                    builder.append('\n');
                    break;
                default:
                }
            }

            for (MonitorInfo monitor : info.getLockedMonitors())
            {
                if (monitor.getLockedStackDepth() == i)
                {
                    builder.append("\t-  locked " + monitor);
                    builder.append('\n');
                }
            }
        }
        if (i < info.getStackTrace().length)
        {
            builder.append("\t...");
            builder.append('\n');
        }

        LockInfo[] locks = info.getLockedSynchronizers();
        if (locks.length > 0)
        {
            builder.append("\n\tNumber of locked synchronizers = " + locks.length);
            builder.append('\n');
            for (LockInfo lock : locks)
            {
                builder.append("\t- " + lock);
                builder.append('\n');
            }
        }
        builder.append('\n');
        return builder.toString();
    }

    private StackTraces()
    {
    }
}
