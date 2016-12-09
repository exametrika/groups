/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link OperationCanceledException} is thrown when asynchronous operation has been canceled.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public class OperationCanceledException extends SystemException
{
    public OperationCanceledException()
    {
        super();
    }

    public OperationCanceledException(ILocalizedMessage message) 
    {
        super(message);
    }

    public OperationCanceledException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }
    
    public OperationCanceledException(Throwable cause) 
    {
        super(cause);
    }
}
