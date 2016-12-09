/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link ChannelException} is thrown when some  communication channel exception occured.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ChannelException extends SystemException
{
    public ChannelException()
    {
    }

    public ChannelException(ILocalizedMessage message)
    {
        super(message);
    }

    public ChannelException(ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
    }

    public ChannelException(Throwable cause)
    {
        super(cause);
    }
}
