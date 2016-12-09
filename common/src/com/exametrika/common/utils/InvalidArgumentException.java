/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link InvalidArgumentException} is thrown when method argument is not valid.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public class InvalidArgumentException extends SystemException
{
    public InvalidArgumentException()
    {
        super();
    }

    public InvalidArgumentException(ILocalizedMessage message) 
    {
        super(message);
    }

    public InvalidArgumentException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }
    
    public InvalidArgumentException(Throwable cause) 
    {
        super(cause);
    }
}
