/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;


/**
 * The {@link EndOfStreamException} is thrown when end of stream has been reached.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class EndOfStreamException extends SerializationException
{
    public EndOfStreamException()
    {
        super();
    }

    public EndOfStreamException(ILocalizedMessage message) 
    {
        super(message);
    }

    public EndOfStreamException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public EndOfStreamException(Throwable cause) 
    {
        super(cause);
    }
}