/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link RawDatabaseException} is thrown when some database exception occured.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawDatabaseException extends SystemException
{
    public RawDatabaseException()
    {
    }

    public RawDatabaseException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawDatabaseException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawDatabaseException(Throwable cause)
    {
        super(cause);
    }
}
