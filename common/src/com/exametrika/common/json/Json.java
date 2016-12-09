/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import com.exametrika.common.utils.Assert;




/**
 * The {@link Json} is helper class for chained generation of Json elements.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Json
{
    private Json parent;
    private IJsonCollection builder;
    private boolean enabled;
    
    public static Json object()
    {
        return new Json(null, new JsonObjectBuilder(), true);
    }
    
    public static Json object(JsonObject object)
    {
        return new Json(null, new JsonObjectBuilder(object), true);
    }
    
    public static Json array()
    {
        return new Json(null, new JsonArrayBuilder(), true);
    }
    
    public static Json array(JsonArray array)
    {
        return new Json(null, new JsonArrayBuilder(array), true);
    }
    
    public static Json builder(JsonObjectBuilder builder)
    {
        return new Json(null, builder, true);
    }
    
    public static Json builder(JsonArrayBuilder builder)
    {
        return new Json(null, builder, true);
    }
    
    public Json putObject(String name)
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonObjectBuilder);
            
            JsonObjectBuilder child = new JsonObjectBuilder();
            ((JsonObjectBuilder)builder).put(name, child);
            return new Json(this, child, true);
        }

        return new Json(this, null, false);
    }
    
    public Json putObjectIf(String name, boolean condition)
    {
        if (enabled && condition)
            return putObject(name);
        
        return new Json(this, null, false);
    }
    
    public Json putArray(String name)
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonObjectBuilder);
            
            JsonArrayBuilder child = new JsonArrayBuilder();
            ((JsonObjectBuilder)builder).put(name, child);
            return new Json(this, child, true);
        }
        
        return new Json(this, null, false);
    }
    
    public Json putArrayIf(String name, boolean condition)
    {
        if (enabled && condition)
            return putArray(name);
        
        return new Json(this, null, false);
    }
    
    public Json put(String name, Object value)
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonObjectBuilder);
            ((JsonObjectBuilder)builder).put(name, JsonUtils.toJson(value));
        }
        return this;
    }
    
    public Json putIf(String name, Object value, boolean condition)
    {
        if (enabled && condition)
            put(name, value);
        
        return this;
    }
    
    public Json addObject()
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonArrayBuilder);
            
            JsonObjectBuilder child = new JsonObjectBuilder();
            ((JsonArrayBuilder)builder).add(child);
            return new Json(this, child, true);
        }
        
        return new Json(this, null, false); 
    }
    
    public Json addObjectIf(boolean condition)
    {
        if (enabled && condition)
            return addObject();
        
        return new Json(this, null, false);
    }
    
    public Json addArray()
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonArrayBuilder);
            
            JsonArrayBuilder child = new JsonArrayBuilder();
            ((JsonArrayBuilder)builder).add(child);
            return new Json(this, child, true);
        }
        
        return new Json(this, null, false);
    }
    
    public Json addArrayIf(boolean condition)
    {
        if (enabled && condition)
            return addArray();
        
        return new Json(this, null, false);
    }
    
    public Json add(Object value)
    {
        if (enabled)
        {
            Assert.checkState(builder instanceof JsonArrayBuilder);
            ((JsonArrayBuilder)builder).add(JsonUtils.toJson(value));
        }
        return this;
    }
    
    public Json addIf(Object value, boolean condition)
    {
        if (enabled && condition)
            add(value);
        
        return this;
    }
    
    public Json end()
    {
        Assert.checkState(parent != null);
        return parent;
    }
    
    public JsonObject toObject()
    {
        Assert.checkState(builder instanceof JsonObjectBuilder);
        return ((JsonObjectBuilder)builder).toJson();
    }
    
    public JsonObjectBuilder toObjectBuilder()
    {
        Assert.checkState(builder instanceof JsonObjectBuilder);
        return ((JsonObjectBuilder)builder);
    }
    
    public JsonArray toArray()
    {
        Assert.checkState(builder instanceof JsonArrayBuilder);
        return ((JsonArrayBuilder)builder).toJson();
    }
    
    public JsonArrayBuilder toArrayBuilder()
    {
        Assert.checkState(builder instanceof JsonArrayBuilder);
        return ((JsonArrayBuilder)builder);
    }
    
    public <T> T builder()
    {
        return (T)builder;
    }
    
    private Json(Json parent, IJsonCollection builder, boolean enabled)
    {
        this.parent = parent;
        this.builder = builder;
        this.enabled = enabled;
    }
}
