/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;


/**
 * The {@link UnsupportedStreamFormatException} is thrown when deserialized stream has format not supported by deserializer.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class UnsupportedStreamFormatException extends SerializationException
{
    public UnsupportedStreamFormatException()
    {
        super();
    }

    public UnsupportedStreamFormatException(ILocalizedMessage message) 
    {
        super(message);
    }

    public UnsupportedStreamFormatException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public UnsupportedStreamFormatException(Throwable cause) 
    {
        super(cause);
    }
}