/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.regex.PatternSyntaxException;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonRegExpValidator} is a validator of regular expressions.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonRegExpValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);

    @Override
    public boolean supports(Class clazz)
    {
        return clazz == String.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        String pattern = (String)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        try
        {
            java.util.regex.Pattern.compile(pattern);
        }
        catch (PatternSyntaxException e)
        {
            diagnostics.addError(messages.invalidPattern(diagnostics.getPath(), pattern, e.getMessage()));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Regular expression pattern ''{1}'' is compiled with error ''{2}''.")
        ILocalizedMessage invalidPattern(String path, String pattern, String error);
    }
}
