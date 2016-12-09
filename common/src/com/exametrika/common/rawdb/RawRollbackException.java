/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link RawRollbackException} is thrown when transaction is rolled back
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawRollbackException extends RawDatabaseException
{
    public RawRollbackException()
    {
    }

    public RawRollbackException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawRollbackException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawRollbackException(Throwable cause)
    {
        super(cause);
    }
}
