/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.ICacheable;




/**
 * The {@link JsonObject} is a JSON object.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class JsonObject implements IJsonCollection<Map.Entry<String, Object>>, Map<String, Object>, ICacheable, Serializable
{
    protected final Map<String, Object> map;
    private int hashCode;
    private final int cacheSize;

    JsonObject(Map<String, Object> map)
    {
        Assert.notNull(map);
        
        this.map = map;
        this.hashCode = map.hashCode();
        this.cacheSize = computeCacheSize(map);
    }
    
    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }
    
    @Override
    public int size()
    {
        return map.size();
    }
    
    public boolean contains(String name)
    {
        Assert.notNull(name);
        
        return map.containsKey(name);
    }
    
    public <T> T get(String name)
    {
        Assert.notNull(name);
        
        T res = (T)map.get(name);
        Assert.notNull(res);
        
        return res;
    }
    
    public <T> T get(String name, Object defaultValue)
    {
        Assert.notNull(name);
        
        defaultValue = JsonUtils.checkValue(defaultValue);
        
        T res = (T)map.get(name);
        if (res != null)
            return res;
        else
            return (T)defaultValue;
    }
   
    @Override
    public <T> T select(String path)
    {
        T res = new JsonPath(path).get(this);
        Assert.notNull(res);
        
        return res;
    }
    
    @Override
    public <T> T select(String path, Object defaultValue)
    {
        defaultValue = JsonUtils.checkValue(defaultValue);
        
        T res = new JsonPath(path).get(this, (T)defaultValue);
        if (res != null)
            return res;
        else
            return (T)defaultValue;
    }
    
    @Override
    public Iterator<Entry<String, Object>> iterator()
    {
        return map.entrySet().iterator();
    }

    @Override
    public boolean containsKey(Object key)
    {
        Assert.notNull(key);
        
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key)
    {
        Assert.notNull(key);
        
        return map.get(key);
    }

    @Override
    public Object put(String key, Object value)
    {
        Assert.notNull(key);
        
        value = JsonUtils.checkValue(value);
        
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key)
    {
        Assert.notNull(key);
        
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map)
    {
        for (Map.Entry<? extends String, ? extends Object> entry : map.entrySet())
            put(entry.getKey(), JsonUtils.checkValue(entry.getValue()));
    }

    @Override
    public void clear()
    {
        map.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return map.keySet();
    }

    @Override
    public Collection<Object> values()
    {
        return map.values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet()
    {
        return map.entrySet();
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof JsonObject))
            return false;
        
        JsonObject instance = (JsonObject)o;
        return map.equals(instance.map);
    }
    
    @Override
    public int hashCode()
    {
        return hashCode;
    }
    
    @Override
    public String toString()
    {
        return JsonSerializers.write(this, true);
    }

    @Override
    public int getCacheSize()
    {
        return cacheSize;
    }
    
    private int computeCacheSize(Map<String, Object> map)
    {
        int cacheSize = CacheSizes.getLinkedHashMapCacheSize(map) + CacheSizes.IMMUTABLES_MAP_CACHE_SIZE;
        for (Map.Entry<String, Object> entry : map.entrySet())
            cacheSize += JsonUtils.getCacheSize(entry.getKey()) + JsonUtils.getCacheSize(entry.getValue());
        
        return cacheSize;
    }
}
