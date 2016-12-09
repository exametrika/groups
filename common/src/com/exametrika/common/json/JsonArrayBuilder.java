/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;




/**
 * The {@link JsonArrayBuilder} is a builder of JSON array.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JsonArrayBuilder extends JsonArray
{
    public JsonArrayBuilder()
    {
        super(new ArrayList<Object>());
    }
    
    public JsonArrayBuilder(JsonArray array)
    {
        super(array != null ? new ArrayList<Object>(array) : new ArrayList<Object>());
    }
    
    public void addAll(JsonArray value)
    {
        Assert.notNull(value);
        
        list.addAll(value.list);
    }
    
    public void update(String path, Object value)
    {
        new JsonPath(path).set(this, value);
    }
    
    public JsonArray toJson()
    {
        List<Object> list = new ArrayList<Object>(this.list.size());
        for (Object element : this.list)
        {
            if (element instanceof JsonArrayBuilder)
                list.add(((JsonArrayBuilder)element).toJson());
            else if (element instanceof JsonObjectBuilder)
                list.add(((JsonObjectBuilder)element).toJson());
            else
                list.add(element);
        }
        
        return new JsonArray(Immutables.wrap(list));
    }
    
    @Override
    public int hashCode()
    {
        return list.hashCode();
    }
}
