/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;

/**
 * The {@link JsonException} is a base JSON exception.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class JsonException extends SystemException
{
    public JsonException()
    {
        super();
    }

    public JsonException(ILocalizedMessage message) 
    {
        super(message);
    }

    public JsonException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public JsonException(Throwable cause) 
    {
        super(cause);
    }
}