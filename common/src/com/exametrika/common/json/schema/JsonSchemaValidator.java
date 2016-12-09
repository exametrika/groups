/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Map;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;





/**
 * The {@link JsonSchemaValidator} is a validator of schema.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonSchemaValidator implements IJsonValidator
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
        JsonObject schema = (JsonObject)instance;
        
        IJsonDiagnostics diagnostics = context.getDiagnostics();
        
        JsonObject types = JsonUtils.get(schema, "types", JsonObject.class, null);
        if (types == null)
            return;
        
        for (Map.Entry<String, Object> entry : types)
        {
            if (!(entry.getValue() instanceof JsonObject))
                continue;
            
            JsonObject valueType = (JsonObject)entry.getValue();
            String instanceOf = JsonUtils.get(valueType, "instanceOf", String.class, null);
            if (instanceOf == null || !instanceOf.equals("object"))
                continue;
            
            StringBuilder builder = new StringBuilder();
            builder.append(entry.getKey());
            
            while (valueType != null)
            {
                String baseReference = JsonUtils.get(valueType, "base", String.class, null);
                if (baseReference == null)
                    break;
                
                builder.append('⟶');
                builder.append(baseReference);
                
                if (baseReference.equals(entry.getKey()))
                {
                    diagnostics.addError(messages.baseTypeCycle(diagnostics.getPath(), entry.getKey(), builder.toString()));
                    break;
                }
                
                valueType = getBase(types, baseReference);
            }
        }
    }
    
    private JsonObject getBase(JsonObject types, String baseReference)
    {
        if (baseReference == null)
            return null;
        
        Object referencedType = types.get(baseReference, null);
        if (!(referencedType instanceof JsonObject))
            return null;

        Object instanceOf = ((JsonObject)referencedType).get("instanceOf", null);
        if (!(instanceOf instanceof String))
            return null;

        String typeName = (String)instanceOf;
        if (!typeName.equals("object"))
            return null;
        
        return (JsonObject)referencedType;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Base type of object type ''{1}'' is type itself: ''{2}''. Cycles are not allowed in type hierarchy.")
        ILocalizedMessage baseTypeCycle(String path, String type, String inheritance);
    }
}
