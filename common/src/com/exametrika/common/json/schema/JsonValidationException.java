/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonException;
import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link JsonValidationException} is a thrown when JSON schema validation fails.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class JsonValidationException extends JsonException
{
    public JsonValidationException()
    {
        super();
    }

    public JsonValidationException(ILocalizedMessage message) 
    {
        super(message);
    }

    public JsonValidationException(ILocalizedMessage message, Throwable cause) 
    {
        super(message, cause);
    }

    public JsonValidationException(Throwable cause) 
    {
        super(cause);
    }
}