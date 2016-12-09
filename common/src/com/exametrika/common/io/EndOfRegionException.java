/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;


/**
 * The {@link EndOfRegionException} is thrown when end of region has been reached.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class EndOfRegionException extends SerializationException
{
    public EndOfRegionException()
    {
        super();
    }

    public EndOfRegionException(ILocalizedMessage message) 
    {
        super(message);
    }

    public EndOfRegionException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public EndOfRegionException(Throwable cause) 
    {
        super(cause);
    }
}