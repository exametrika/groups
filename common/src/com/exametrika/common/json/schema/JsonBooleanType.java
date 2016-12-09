/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.Set;






/**
 * The {@link JsonBooleanType} is a JSON boolean type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonBooleanType extends JsonType
{
    public JsonBooleanType(String name, String description, IJsonConverter converter)
    {
        super(name, description, null, null, null, converter);
    }

    @Override
    public boolean supports(Object instance)
    {
        return instance instanceof Boolean || (instance instanceof String && (instance.equals("true") || instance.equals("false")));
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("boolean");
    }
    
    @Override
    protected Object doValidate(Object instance, JsonValidationContext context)
    {
        super.doValidate(instance, context);
        
        if (instance instanceof String)
            return instance.equals("true");
        else
            return instance;
    }
}
