/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;




/**
 * The {@link JsonObjectType} is a JSON object type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonObjectType extends JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private List<JsonProperty> properties;
    private JsonObjectType base;
    private final boolean open;
    private final boolean isAbstract;
    private final boolean isFinal;

    public JsonObjectType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators, 
        IJsonConverter converter, boolean open, boolean isAbstract, boolean isFinal)
    {
        super(name, description, enumeration, annotation, validators, converter);
        
        Assert.isTrue(!isAbstract || !isFinal);
        Assert.isTrue(enumeration == null || isFinal);
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
            {
                Assert.isTrue(validator.supports(JsonObject.class));
                Assert.isTrue(validator.supports(JsonObjectBuilder.class));
            }
        }

        this.open = open;
        this.isAbstract = isAbstract;
        this.isFinal = isFinal;
    }
    
    public List<JsonProperty> getProperties()
    {
        return properties;
    }

    public JsonObjectType getBase()
    {
        return base;
    }

    public boolean isOpen()
    {
        return open;
    }
    
    public boolean isAbstract()
    {
        return isAbstract;
    }
    
    public boolean isFinal()
    {
        return isFinal;
    }
    
    public void setBase(JsonObjectType base)
    {
        Assert.isTrue(base == null || !base.isFinal());
        Assert.isTrue(!open || base == null || base.isOpen());
        Assert.checkState(!frozen);
        
        this.base = base;
        
        while (base != null)
        {
            Assert.isTrue(base != this);
            base = base.base;
        }
    }
    
    public void setProperties(List<JsonProperty> properties)
    {
        Assert.notNull(properties);
        Assert.checkState(!frozen);
        
        this.properties = Immutables.wrap(properties);
    }
    
    @Override
    public void freeze()
    {
        if (!frozen)
        {
            super.freeze();
            Assert.checkState(properties != null);
            
            if (base != null)
                base.freeze();
            
            for (JsonProperty property : properties)
                property.getType().freeze();
        }
    }
    
    @Override
    public boolean supports(Object instance)
    {
        return instance instanceof JsonObject;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("object");
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        JsonObjectBuilder object = (JsonObjectBuilder)JsonUtils.toBuilder(instance);
        instance = object;
        Object instanceOf = object.get("instanceOf", null);
        String typeName = null;
        if (instanceOf instanceof String)
            typeName = (String)instanceOf;
        else if (instanceOf != null)
        {
            diagnostics.addError(messages.wrongInstanceOfType(diagnostics.getPath()));
            return instance;
        }
        
        if (typeName != null && !typeName.equals(getName()))
        {
            JsonType type = context.getSchema().findType(typeName);
            if (type == null || !(type instanceof JsonObjectType))
            {
                diagnostics.addError(messages.typeNotFound(diagnostics.getPath(), typeName));
                return instance;
            }
            else
            {
                JsonObjectType objectType = (JsonObjectType)type;
                
                if (!isBaseOrEqual(objectType))
                {
                    diagnostics.addError(messages.typeNotDescendant(diagnostics.getPath(), typeName, getName()));
                    return instance;
                }
                else
                    return type.validate(instance, context);
            }
        }
        else
        {
            if (isAbstract)
                diagnostics.addError(messages.abstractTypeInstantiation(diagnostics.getPath(), getName()));
            
            Set<String> typeProperties = new HashSet<String>();
            JsonObjectType objectType = this;
            while (objectType != null)
            {
                for (JsonProperty property : objectType.properties)
                {
                    if (!typeProperties.contains(property.getName()))
                    {
                        property.validate(this, instance, context);
                        typeProperties.add(property.getName());
                    }
                }
                
                if (objectType != this && objectType.getValidators() != null)
                {
                    for (IJsonValidator validator : objectType.getValidators())
                    {
                        if (validator.supports(instance.getClass()))
                            validator.validate(this, instance, context);
                    }
                }
                
                objectType = objectType.base;
            }
            
            if (!open)
            {
                for (Map.Entry<String, Object> entry : object)
                {
                    String name = entry.getKey();
                    if (!name.equals("instanceOf") && !typeProperties.contains(name))
                        diagnostics.addError(messages.propertyUndefined(diagnostics.getPath(), entry.getKey(), getName()));
                }
            }
        }
        
        return instance;
    }
    
    private boolean isBaseOrEqual(JsonObjectType type)
    {
        Assert.notNull(type);
        
        if (type == this)
            return true;
        
        if (type.getBase() == null)
            return false;
        
        return isBaseOrEqual(type.getBase());
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Object type ''{1}'' is not found.")
        ILocalizedMessage typeNotFound(String path, String type);
        @DefaultMessage("Validation error of ''{0}''. Object type ''{1}'' is not a descendant of type ''{2}''.")
        ILocalizedMessage typeNotDescendant(String path, String type, String base);
        @DefaultMessage("Validation error of ''{0}''. Object type ''{1}'' is abstract. Object type can not be abstract.")
        ILocalizedMessage abstractTypeInstantiation(String path, String type);
        @DefaultMessage("Validation error of ''{0}''. Object property ''{1}'' is undefined. Object type ''{2}'' is close and does not permit undefined properties in object instances.")
        ILocalizedMessage propertyUndefined(String path, String propertyName, String type);
        @DefaultMessage("Validation error of ''{0}''. Type of object property ''instanceOf'' must be a string.")
        ILocalizedMessage wrongInstanceOfType(String path);
    }
}
