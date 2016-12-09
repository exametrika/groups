/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;







/**
 * The {@link JsonSuffixLongConverter} is a JSON suffix long converter.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class JsonSuffixLongConverter implements IJsonConverter
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<Pair<String, Long>> suffixes;
    
    public JsonSuffixLongConverter(List<Pair<String, Long>> suffixes)
    {
        Assert.notNull(suffixes);
        
        this.suffixes = suffixes;
    }
    
    @Override
    public final Object convert(JsonType type, String value, JsonValidationContext context)
    {
        int pos = value.length();
        for (int i = 0; i < value.length(); i++)
        {
            if (!Character.isDigit(value.charAt(i)))
            {
                pos = i;
                break;
            }
        }
        
        String suffix = null;
        if (pos < value.length())
            suffix = value.substring(pos);

        try
        {
            long l = Long.parseLong(value.substring(0, pos));
            if (suffix == null)
                return l;
            else
                return multiply(l, suffix, value, context);
        }
        catch (NumberFormatException e)
        {
            context.getDiagnostics().addError(messages.wrongLongValue(context.getDiagnostics().getPath(), value, suffixes));
            return value;
        }
    }

    private Object multiply(long l, String suffix, String value, JsonValidationContext context)
    {
        for (Pair<String, Long> pair : suffixes)
        {
            if (pair.getKey().equalsIgnoreCase(suffix))
            {
                if (pair.getValue() > 0)
                    return l * pair.getValue();
                else
                    return l / -pair.getValue();
            }
        }
        
        context.getDiagnostics().addError(messages.wrongSuffix(context.getDiagnostics().getPath(), suffix, suffixes));
        return value;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. The following instance has wrong suffix ''{1}''." + 
            " Expected one of the following values: {2}.")
        ILocalizedMessage wrongSuffix(String path, String suffix, List<Pair<String, Long>> suffixes);
        
        @DefaultMessage("Validation error of ''{0}''. The following instance has wrong long value ''{1}''." + 
            " Expected long value with optional suffix: {2}.")
        ILocalizedMessage wrongLongValue(String path, String value, List<Pair<String, Long>> suffixes);
    }
}
