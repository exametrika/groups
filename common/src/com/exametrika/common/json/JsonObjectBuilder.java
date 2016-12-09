/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;




/**
 * The {@link JsonObjectBuilder} is a builder of JSON object.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonObjectBuilder extends JsonObject
{
    public JsonObjectBuilder()
    {
        super(new LinkedHashMap<String, Object>());
    }
    
    public JsonObjectBuilder(JsonObject object)
    {
        super(object != null ? new LinkedHashMap<String, Object>(object) : new LinkedHashMap<String, Object>());
    }
    
    public void putAll(JsonObject value)
    {
        Assert.notNull(value);
        
        map.putAll(value.map);
    }
    
    public void append(String name, Object value)
    {
        Assert.notNull(name);
        
        value = JsonUtils.checkValue(value);
        
        Object existingValue = map.get(name);
        if (existingValue == null)
            map.put(name, value);
        else if (existingValue instanceof JsonArrayBuilder)
            ((JsonArrayBuilder)existingValue).add(value);
        else
        {
            JsonArrayBuilder listBuilder;
            
            if (existingValue instanceof JsonArray)
                listBuilder = new JsonArrayBuilder((JsonArray)existingValue);
            else
            {
                listBuilder = new JsonArrayBuilder();
                listBuilder.add(existingValue);
            }
            
            listBuilder.add(value);
            
            map.put(name, listBuilder);
        }
    }
    
    public void add(String name, Object value)
    {
        Assert.notNull(name);
        
        value = JsonUtils.checkValue(value);
        
        Object existingValue = map.get(name);
        if (existingValue instanceof JsonArrayBuilder)
            ((JsonArrayBuilder)existingValue).add(value);
        else
        {
            JsonArrayBuilder listBuilder;
            
            if (existingValue instanceof JsonArray)
                listBuilder = new JsonArrayBuilder((JsonArray)existingValue);
            else
            {
                listBuilder = new JsonArrayBuilder();
                
                if (existingValue != null)
                    listBuilder.add(existingValue);
            }
            
            listBuilder.add(value);
            
            map.put(name, listBuilder);
        }
    }
    
    public void update(String path, Object value)
    {
        new JsonPath(path).set(this, value);
    }
    
    public JsonObject toJson()
    {
        Map<String, Object> map = new LinkedHashMap<String, Object>(this.map.size());
        for (Map.Entry<String, Object> entry : this.map.entrySet())
        {
            if (entry.getValue() instanceof JsonObjectBuilder)
                map.put(entry.getKey(), ((JsonObjectBuilder)entry.getValue()).toJson());
            else if (entry.getValue() instanceof JsonArrayBuilder)
                map.put(entry.getKey(), ((JsonArrayBuilder)entry.getValue()).toJson());
            else
                map.put(entry.getKey(), entry.getValue());
        }
        
        return new JsonObject(Immutables.wrap(map));
    }
    
    @Override
    public int hashCode()
    {
        return map.hashCode();
    }
}
