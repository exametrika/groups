/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link RawTransactionReadOnlyException} is thrown when attempt to write to read-only transaction is requested.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RawTransactionReadOnlyException extends RawDatabaseException
{
    public RawTransactionReadOnlyException()
    {
    }

    public RawTransactionReadOnlyException(ILocalizedMessage message)
    {
        super(message);
    }

    public RawTransactionReadOnlyException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public RawTransactionReadOnlyException(Throwable cause)
    {
        super(cause);
    }
}
