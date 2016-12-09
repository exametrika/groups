/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
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




/**
 * The {@link JsonMapType} is a JSON map type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonMapType extends JsonType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int minCount;
    private final int maxCount;
    private final boolean allowNulls;
    private JsonStringType keyType;
    private JsonType valueType;

    public JsonMapType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators, 
        IJsonConverter converter, int minCount, int maxCount, boolean allowNulls)
    {
        super(name, description, enumeration, annotation, validators, converter);
        
        if (validators != null)
        {
            for (IJsonValidator validator : validators)
            {
                Assert.isTrue(validator.supports(JsonObject.class));
                Assert.isTrue(validator.supports(JsonObjectBuilder.class));
            }
        }
        
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.allowNulls = allowNulls;
    }
    
    public int getMinCount()
    {
        return minCount;
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    public boolean isAllowNulls()
    {
        return allowNulls;
    }

    public JsonStringType getKeyType()
    {
        return keyType;
    }
    
    public JsonType getValueType()
    {
        return valueType;
    }

    public void setKeyType(JsonStringType keyType)
    {
        Assert.notNull(keyType);
        Assert.checkState(!frozen);
        
        this.keyType = keyType;
    }
    
    public void setValueType(JsonType valueType)
    {
        Assert.notNull(valueType);
        Assert.checkState(!frozen);
        
        this.valueType = valueType;
    }
    
    @Override
    public void freeze()
    {
        if (!frozen)
        {
            super.freeze();
            Assert.checkState(keyType != null && valueType != null);
            
            keyType.freeze();
            valueType.freeze();
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
        
        int size = object.size();

        if (size < minCount)
            diagnostics.addError(messages.countLessMin(diagnostics.getPath(), size, minCount));
        
        if (size > maxCount)
            diagnostics.addError(messages.countGreaterMax(diagnostics.getPath(), size, maxCount));
        
        for (Map.Entry<String, Object> entry : object)
        {
            diagnostics.beginProperty(entry.getKey());
            
            diagnostics.beginProperty("key");
            keyType.validate(entry.getKey(), context);
            diagnostics.end();
            
            Object value = entry.getValue();
            
            if (value != null)
            {
                context.beginObjectElement(this, object, entry.getKey());
                value = valueType.validate(value, context);
                context.endElement();
                
                entry.setValue(value);
            }
            else if (!allowNulls)
                diagnostics.addError(messages.nullElement(diagnostics.getPath()));
            
            diagnostics.end();
        }
        
        return instance;
    }

    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Map element value can not be null.")
        ILocalizedMessage nullElement(String path);

        @DefaultMessage("Validation error of ''{0}''. Count of map elements ''{1}'' is greater than maximal allowed ''{2}''.")
        ILocalizedMessage countGreaterMax(String path, int count, int maxCount);

        @DefaultMessage("Validation error of ''{0}''. Count of map elements ''{1}'' is less than minimal allowed ''{2}''.")
        ILocalizedMessage countLessMin(String path, int count, int minCount);
    }
}
