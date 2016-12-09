/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.rawdb.RawDatabaseException;

/**
 * The {@link RawClearCacheException} is thrown when all database caches must be cleared.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawClearCacheException extends RawDatabaseException
{
    public RawClearCacheException()
    {
    }

    public RawClearCacheException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawClearCacheException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawClearCacheException(Throwable cause)
    {
        super(cause);
    }
}
