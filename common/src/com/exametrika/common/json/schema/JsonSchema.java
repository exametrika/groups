/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;




/**
 * The {@link JsonSchema} is a JSON schema.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonSchema
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final String description;
    private final List<JsonType> types;
    private final Map<String, JsonType> typesMap;

    public JsonSchema(String name, String description, List<JsonType> types)
    {
        Assert.notNull(name);
        Assert.notNull(description);
        Assert.notNull(types);
        
        Map<String, JsonType> typesMap = new HashMap<String, JsonType>(types.size());
        for (JsonType type : types)
        {
            type.freeze();
            typesMap.put(type.getName(), type);
        }
        
        this.name = name;
        this.description = description;
        this.types = Immutables.wrap(types);
        this.typesMap = typesMap;
        
        JsonDiagnostics diagnostics = new JsonDiagnostics(); 
        for (JsonType type : types)
            validateType(diagnostics, type, true);
        
        diagnostics.checkErrors();
    }

    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public List<JsonType> getTypes()
    {
        return types;
    }
    
    public JsonType findType(String name)
    {
        return typesMap.get(name);
    }

    public void validate(Object instance, String typeName)
    {
        Assert.notNull(instance);
        Assert.notNull(typeName);
        
        instance = JsonUtils.checkValue(instance);
        
        JsonType type = typesMap.get(typeName);
        if (type == null)
            throw new JsonValidationException(messages.typeNotFound(name, typeName));

        JsonDiagnostics diagnostics = new JsonDiagnostics();
        JsonValidationContext context = new JsonValidationContext(this, diagnostics, type, instance);
        
        diagnostics.beginType(type.getName());
        type.validate(instance, context);
        diagnostics.end();
            
        diagnostics.checkErrors();
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    private void validateType(JsonDiagnostics diagnostics, JsonType type, boolean topLevel)
    {
        if (!topLevel && !type.getName().isEmpty())
            return;
        
        if (topLevel)
            diagnostics.beginType(type.getName());
        if (type.getEnumeration() != null)
        {
            diagnostics.beginProperty("enumeration");
            for (int i = 0; i < type.getEnumeration().size(); i++)
            {
                diagnostics.beginIndex(i);
                Object instance = type.getEnumeration().get(i);
                JsonValidationContext context = new JsonValidationContext(this, diagnostics, type, instance);
                type.validate(instance, context);
                diagnostics.end();
            }
            diagnostics.end();
        }
        
        if (type instanceof JsonArrayType)
        {
            diagnostics.beginProperty("elementType");
            validateType(diagnostics, ((JsonArrayType)type).getElementType(), false);
            diagnostics.end();
        }
        else if (type instanceof JsonMapType)
        {
            diagnostics.beginProperty("keyType");
            validateType(diagnostics, ((JsonMapType)type).getKeyType(), false);
            diagnostics.end();
            diagnostics.beginProperty("valueType");
            validateType(diagnostics, ((JsonMapType)type).getValueType(), false);
            diagnostics.end();
        }
        else if (type instanceof JsonCompoundType)
        {
            JsonCompoundType compoundType = (JsonCompoundType)type;
            diagnostics.beginProperty("types");
            for (int i = 0; i < compoundType.getTypes().size(); i++)
            {
                diagnostics.beginIndex(i);
                JsonType subType = compoundType.getTypes().get(i);
                validateType(diagnostics, subType, false);
                diagnostics.end();
            }
            diagnostics.end();
        }
        else if (type instanceof JsonObjectType)
        {
            for (JsonProperty property : ((JsonObjectType)type).getProperties())
            {
                diagnostics.beginProperty(property.getName());
                validateType(diagnostics, property.getType(), false);
                
                diagnostics.beginProperty("defaultValue");
                if (property.getDefaultValue() != null)
                {
                    Object defaultValue = property.getDefaultValue();
                    JsonValidationContext context = new JsonValidationContext(this, diagnostics, property.getType(), defaultValue);
                    property.getType().validate(defaultValue, context);
                }
                diagnostics.end();
                diagnostics.end();
            }
        }
        if (topLevel)
            diagnostics.end();
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of schema ''{0}''. Type ''{1}'' is not found.")
        ILocalizedMessage typeNotFound(String schemaName, String type);
    }
}
