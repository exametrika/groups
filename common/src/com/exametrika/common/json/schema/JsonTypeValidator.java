/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonTypeValidator} is a validator of {@link JsonType}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonTypeValidator implements IJsonValidator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<String> validators;

    public JsonTypeValidator(Set<String> validators)
    {
        this.validators = validators;
    }
    
    @Override
    public boolean supports(Class clazz)
    {
        return clazz == JsonObject.class || clazz == JsonObjectBuilder.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context)
    {
        JsonObject instanceType = (JsonObject)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        if (!instanceType.contains("instanceOf"))
            diagnostics.addError(messages.schemaElementTypeNotSet(diagnostics.getPath()));
        
        if (validators != null)
        {
            JsonArray typeValidators = instanceType.get("validators", null);
            if (typeValidators != null)
            {
                for (Object validator : typeValidators)
                {
                    if (validator instanceof String && !validators.contains(validator))
                        diagnostics.addError(messages.validatorNotFound(diagnostics.getPath(), (String)validator));
                }
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Type of schema element is not set.")
        ILocalizedMessage schemaElementTypeNotSet(String path);
        
        @DefaultMessage("Validation error of ''{0}''. Type validator ''{1}'' is not found.")
        ILocalizedMessage validatorNotFound(String path, String validatorName);
    }
}
