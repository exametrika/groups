/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;







/**
 * The {@link JsonPercentDoubleConverter} is a JSON percent double converter.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonPercentDoubleConverter implements IJsonConverter
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public Object convert(JsonType type, String value, JsonValidationContext context)
    {
        try
        {
            if (value.charAt(value.length() - 1) == '%')
                return Double.parseDouble(value.substring(0, value.length() - 1)) / 100;
            else
                return Double.parseDouble(value);
        }
        catch (NumberFormatException e)
        {
            context.getDiagnostics().addError(messages.wrongDoubleValue(context.getDiagnostics().getPath(), value));
            return value;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. The following instance has wrong double value ''{1}''." + 
            " Expected double value with optional percent sign.")
        ILocalizedMessage wrongDoubleValue(String path, String value);
    }
}
