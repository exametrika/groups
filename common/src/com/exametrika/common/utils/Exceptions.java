/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.tasks.ThreadInterruptedException;


/**
 * The {@link Exceptions} contains different utility methods for exception manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Exceptions
{
    /**
     * Checks {@link Thread#isInterrupted} status of thread and throws {@link ThreadInterruptedException} if
     * interrupted status is set.
     *
     * @param exception exception to check
     */
    public static void checkInterrupted(Exception exception)
    {
        if (exception instanceof ThreadInterruptedException)
            throw (ThreadInterruptedException)exception;
        else if (exception instanceof InterruptedException)
            throw new ThreadInterruptedException((InterruptedException)exception);
        else if (Thread.currentThread().isInterrupted())
            throw new ThreadInterruptedException(exception);
    }
    
    /**
     * Wraps application exception to system one and throws resulting exception.
     *
     * @param e application exception
     * @return never happened
     */
    public static <T> T wrapAndThrow(Throwable e)
    {
        Assert.notNull(e);
        
        if (e instanceof RuntimeException)
            throw (RuntimeException)e;
        
        throw new SystemException(e);
    }
    
    /**
     * If system exception contains application exception, unwraps and throws it, if does not contain throws original exception.
     *
     * @param <T> application exception type
     * @param clazz application exception clazz
     * @param e system exception
     * @exception T if specified exception contains application exception, else throws original exception
     */
    public static <T extends Throwable> void uwrapAndThrow(Class<T> clazz, Exception e) throws T
    {
        Assert.notNull(clazz);
        Assert.notNull(e);
        
        if (clazz.isInstance(e))
            throw (T)e;
            
        if (e.getCause() != null && clazz.isInstance(e.getCause()))
            throw (T)e.getCause();
    }
    
    /**
     * If system exception contains application exception, unwraps and throws it, if does not contain throws original exception.
     *
     * @param <T1> application exception type 1
     * @param <T2> application exception type 2
     * @param clazz1 application exception clazz 1
     * @param clazz2 application exception clazz 2
     * @param e system exception
     * @exception T1, T2 if specified exception contains application exception, else throws original exception
     */
    public static <T1 extends Throwable, T2 extends Throwable> void uwrapAndThrow(Class<T1> clazz1, Class<T2> clazz2, 
        Exception e) throws T1, T2
    {
        Assert.notNull(clazz1);
        Assert.notNull(clazz2);
        Assert.notNull(e);
        
        if (clazz1.isInstance(e))
            throw (T1)e;
        if (clazz2.isInstance(e))
            throw (T2)e;
            
        if (e.getCause() != null && clazz1.isInstance(e.getCause()))
            throw (T1)e.getCause();
        if (e.getCause() != null && clazz2.isInstance(e.getCause()))
            throw (T2)e.getCause();
    }
    
    public static String getStackTrace(Throwable e)
    {
        if (e == null)
            return null;
        
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        
        return writer.toString();
    }

    private Exceptions()
    {
    }
}
