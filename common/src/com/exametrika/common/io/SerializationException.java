/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link SerializationException} is a base exception for serialization framework.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class SerializationException extends SystemException
{
    public SerializationException()
    {
        super();
    }

    public SerializationException(ILocalizedMessage message) 
    {
        super(message);
    }

    public SerializationException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public SerializationException(Throwable cause) 
    {
        super(cause);
    }
}