/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link RawFileNotFoundException} is thrown when file is not found in read-only transaction.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawFileNotFoundException extends RawDatabaseException
{
    public RawFileNotFoundException()
    {
    }

    public RawFileNotFoundException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawFileNotFoundException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawFileNotFoundException(Throwable cause)
    {
        super(cause);
    }
}
