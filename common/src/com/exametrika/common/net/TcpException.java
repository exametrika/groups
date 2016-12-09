/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link TcpException} is thrown when TCP exception occured.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class TcpException extends SystemException
{
    public TcpException()
    {
    }

    public TcpException(ILocalizedMessage message)
    {
        super(message);
    }

    public TcpException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public TcpException(Throwable cause)
    {
        super(cause);
    }
}
