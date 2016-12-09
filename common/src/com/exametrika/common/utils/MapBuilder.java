/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.LinkedHashMap;
import java.util.Map;




/**
 * The {@link MapBuilder} represents a of {@link Map}.
 * 
 * @param <K> key type
 * @param <V> value type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class MapBuilder<K, V>
{
    private final Map<K, V> map;
    
    public MapBuilder()
    {
        map = new LinkedHashMap<K, V>();
    }
    
    public MapBuilder(Map<K, V> map)
    {
        this.map = new LinkedHashMap<K, V>(map);
    }
    
    public MapBuilder<K, V> put(K key, V value)
    {
        map.put(key, value);
        return this;
    }
    
    public Map<K, V> toMap()
    {
        return new LinkedHashMap<K, V>(map);
    }
}
