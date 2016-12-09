/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.List;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;





/**
 * The {@link JsonType} is a abstract JSON type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final String description;
    private final JsonArray enumeration;
    private final JsonObject annotation;
    private final List<IJsonValidator> validators;
    private final IJsonConverter converter;
    protected boolean frozen;

    public JsonType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators,
        IJsonConverter converter)
    {
        Assert.notNull(name);
        Assert.notNull(description);
        
        this.name = name;
        this.description = description;
        this.enumeration = enumeration;
        this.annotation = annotation;
        this.validators = Immutables.wrap(validators);
        this.converter = converter;
    }
    
    public final String getName()
    {
        return name;
    }

    public final String getDescription()
    {
        return description;
    }
    
    public final JsonArray getEnumeration()
    {
        return enumeration;
    }
    
    public final JsonObject getAnnotation()
    {
        return annotation;
    }
    
    public final List<IJsonValidator> getValidators()
    {
        return validators;
    }
    
    public final IJsonConverter getConverter()
    {
        return converter;
    }

    public abstract boolean supports(Object instance);

    public final Object validate(Object instance, JsonValidationContext context)
    {
        Assert.notNull(instance);
        Assert.notNull(context);
        
        if (converter != null && instance instanceof String)
            instance = converter.convert(this, (String)instance, context);
        
        if (!supports(instance))
        {
            context.getDiagnostics().addError(messages.wrongInstanceType(context.getDiagnostics().getPath(), getType(instance.getClass()),
                Strings.indent(instance.toString(), 8), getSupportedTypes().toString()));
            return instance;
        }
        else
            return doValidate(instance, context);
    }

    public void freeze()
    {
        frozen = true;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    protected abstract Set<String> getSupportedTypes();
    
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        if (enumeration != null)
        {
            if (enumeration.indexOf(instance) == -1)
                context.getDiagnostics().addError(messages.instanceNotInEnumeration(context.getDiagnostics().getPath(), Strings.indent(instance.toString(), 8),
                    Strings.indent(enumeration.toString(), 8)));
        }
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
            {
                if (validator.supports(instance.getClass()))
                    validator.validate(this, instance, context);
            }
        }
        
        return instance;
    }
    
    protected final String getType(Class clazz)
    {
        if (clazz == JsonArray.class || clazz == JsonArrayBuilder.class)
            return "array";
        else if (clazz == JsonObject.class || clazz == JsonObjectBuilder.class)
            return "object";
        else if (clazz == Long.class)
            return "long";
        else if (clazz == Double.class)
            return "double";
        else if (clazz == String.class)
            return "string";
        else if (clazz == Boolean.class)
            return "boolean";
        else
            return "unsupported";
    }

    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. The following instance is not in enumeration:\n{1}.\n    Instance must be one of the enumeration elements: \n{2}.")
        ILocalizedMessage instanceNotInEnumeration(String path, String instance, String enumeration);
        @DefaultMessage("Validation error of ''{0}''. The following instance has wrong type ''{1}'':\n{2}.\n    Expected one of the following types: {3}.")
        ILocalizedMessage wrongInstanceType(String path, String type, String instance, String types);
    }
}
