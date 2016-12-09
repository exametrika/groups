/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonPropertyValidator} is a validator of object properties.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonPropertyValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    
    @Override
    public boolean supports(Class clazz)
    {
        return clazz == JsonObject.class || clazz == JsonObjectBuilder.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        JsonObject property = (JsonObject)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        
        boolean required = false;
        Object requiredElement = property.get("required", null);
        if (requiredElement == null || (requiredElement instanceof Boolean && (Boolean)requiredElement))
            required = true;
        boolean allowed = false;
        Object allowedElement = property.get("allowed", null);
        if (allowedElement == null || (allowedElement instanceof Boolean && (Boolean)allowedElement))
            allowed = true;
            
        if (!allowed && required)
            diagnostics.addError(messages.allowedAndRequired(diagnostics.getPath()));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Required property must be allowed.")
        ILocalizedMessage allowedAndRequired(String path);
    }
}
