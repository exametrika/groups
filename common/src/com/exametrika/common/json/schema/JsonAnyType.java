/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;





/**
 * The {@link JsonAnyType} is a JSON any type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonAnyType extends JsonType
{
    public JsonAnyType(String name, String description, JsonArray enumeration, JsonObject annotation, List<IJsonValidator> validators,
        IJsonConverter converter)
    {
        super(name, description, enumeration, annotation, validators, converter);
    }
    
    @Override
    public boolean supports(Object instance)
    {
        return true;
    }
    
    @Override
    protected Set<String> getSupportedTypes()
    {
        return Collections.singleton("any");
    }
}
