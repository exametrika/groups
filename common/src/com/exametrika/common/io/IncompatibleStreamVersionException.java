/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;


/**
 * The {@link IncompatibleStreamVersionException} is thrown when deserialized stream has version incompatible with deserializer.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class IncompatibleStreamVersionException extends SerializationException
{
    public IncompatibleStreamVersionException()
    {
        super();
    }

    public IncompatibleStreamVersionException(ILocalizedMessage message) 
    {
        super(message);
    }

    public IncompatibleStreamVersionException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public IncompatibleStreamVersionException(Throwable cause) 
    {
        super(cause);
    }
}