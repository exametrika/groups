/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;







/**
 * The {@link JsonBooleanConverter} is a JSON boolean converter.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonBooleanConverter implements IJsonConverter
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public Object convert(JsonType type, String value, JsonValidationContext context)
    {
        if (value.equals("true") || value.equals("yes") || value.equals("on"))
            return true;
        else if (value.equals("false") || value.equals("no") || value.equals("off"))
            return false;
        else
        {
            context.getDiagnostics().addError(messages.wrongBooleanValue(context.getDiagnostics().getPath(), value));
            return value;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. The following instance has wrong boolean value ''{1}''." + 
            " Expected one of the following values: true/false, yes/no, on/off.")
        ILocalizedMessage wrongBooleanValue(String path, String value);
    }
}
