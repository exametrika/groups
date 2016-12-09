/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link InvalidStateException} is thrown when object state is not valid.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public class InvalidStateException extends SystemException
{
    public InvalidStateException()
    {
        super();
    }

    public InvalidStateException(ILocalizedMessage message) 
    {
        super(message);
    }

    public InvalidStateException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }
    
    public InvalidStateException(Throwable cause) 
    {
        super(cause);
    }
}
