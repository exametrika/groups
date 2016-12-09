/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;




/**
 * The {@link JsonProperty} is a JSON object property.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonProperty
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final String description;
    private final JsonType type;
    private final boolean required;
    private final boolean allowed;
    private final Object defaultValue;

    public JsonProperty(String name, String description, JsonType type, boolean required, boolean allowed, Object defaultValue)
    {
        Assert.notNull(name);
        Assert.notNull(description);
        Assert.notNull(type);
        Assert.isTrue(allowed || !required);
        
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.allowed = allowed;
        this.defaultValue = JsonUtils.toImmutable(JsonUtils.checkValue(defaultValue));
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }

    public JsonType getType()
    {
        return type;
    }

    public boolean isRequired()
    {
        return required;
    }
    
    public boolean isAllowed()
    {
        return allowed;
    }
    
    public Object getDefaultValue()
    {
        return defaultValue;
    }
    
    public final void validate(JsonObjectType objectType, Object instance, JsonValidationContext context)
    {
        Assert.notNull(instance);
        Assert.notNull(context);

        JsonDiagnostics diagnostics = context.getDiagnostics();
        
        diagnostics.beginProperty(name);
        
        JsonObjectBuilder object = (JsonObjectBuilder)instance;

        if (!allowed && object.contains(name))
            diagnostics.addError(messages.notAllowedPropertyValue(diagnostics.getPath()));
        
        Object value, initialValue = null;
        if (object.containsKey(name))
        {
            value = object.get(name, null);
            initialValue = value;
        }
        else
            value = defaultValue;
        
        if (value != null)
        {
            context.beginObjectElement(objectType, object, name);
            value = type.validate(value, context);
            context.endElement();
            
            if (value != initialValue)
                object.put(name, value);
        }
        else if (required)
            diagnostics.addError(messages.nullInRequiredProperty(diagnostics.getPath()));
        
        diagnostics.end();
    }
    
    @Override
    public String toString()
    {
        return name;
    }

    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Value of required property can not be null.")
        ILocalizedMessage nullInRequiredProperty(String path);
        
        @DefaultMessage("Validation error of ''{0}''. Value of disallowed property must not be set explicitly.")
        ILocalizedMessage notAllowedPropertyValue(String path);
    }
}
