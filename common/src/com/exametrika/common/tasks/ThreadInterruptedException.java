/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link ThreadInterruptedException} is thrown when thread is interrupted by another thread.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ThreadInterruptedException extends SystemException
{
    public ThreadInterruptedException()
    {
        super();
    }

    public ThreadInterruptedException(ILocalizedMessage message) 
    {
        super(message);
    }

    public ThreadInterruptedException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public ThreadInterruptedException(InterruptedException cause) 
    {
        super(cause);
        
        // Restore thread interruption status
        Thread.currentThread().interrupt();
    }
    
    public ThreadInterruptedException(Throwable cause) 
    {
        super(cause);
    }
}