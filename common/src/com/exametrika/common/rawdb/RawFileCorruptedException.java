/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link RawFileCorruptedException} is thrown when end-of-file reached while reading page data.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawFileCorruptedException extends RawDatabaseException
{
    public RawFileCorruptedException()
    {
    }

    public RawFileCorruptedException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawFileCorruptedException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawFileCorruptedException(Throwable cause)
    {
        super(cause);
    }
}
