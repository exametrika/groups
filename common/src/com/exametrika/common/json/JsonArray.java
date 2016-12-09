/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.ICacheable;




/**
 * The {@link JsonArray} is a JSON array.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class JsonArray implements IJsonCollection<Object>, List<Object>, RandomAccess, ICacheable, Serializable
{
    protected final List<Object> list;
    private final int hashCode;
    private final int cacheSize;

    JsonArray(List<Object> list)
    {
        Assert.notNull(list);
        
        this.list = list;
        this.hashCode = list.hashCode();
        this.cacheSize = computeCacheSize(list);
    }
    
    @Override
    public Object get(int index)
    {
        Object res = list.get(index);
        Assert.notNull(res);
        
        return res;
    }
    
    public <T> T get(int index, Object defaultValue)
    {
        defaultValue = JsonUtils.checkValue(defaultValue);
        
        T res = (T)list.get(index);
        if (res != null)
            return res;
        else
            return (T)defaultValue;
    }
    
    @Override
    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }
    
    @Override
    public boolean isEmpty()
    {
        return list.isEmpty();
    }
    
    @Override
    public int size()
    {
        return list.size();
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
        
        T res = new JsonPath(path).get(this);
        if (res != null)
            return res;
        else
            return (T)defaultValue;
    }

    @Override
    public Iterator<Object> iterator()
    {
        return list.iterator();
    }
    
    @Override
    public boolean contains(Object o)
    {
        return list.contains(o);
    }

    @Override
    public Object[] toArray()
    {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return list.toArray(a);
    }

    @Override
    public boolean add(Object e)
    {
        e = JsonUtils.checkValue(e);
        
        return list.add(e);
    }

    @Override
    public boolean remove(Object o)
    {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Object> c)
    {
        boolean changed = false;
        for (Object o : c)
        {
            add(o);
            changed = true;
        }
        
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Object> c)
    {
        boolean changed = false;
        for (Object o : c)
        {
            add(index++, o);
            changed = true;
        }
        
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return list.retainAll(c);
    }

    @Override
    public void clear()
    {
        list.clear();
    }

    @Override
    public Object set(int index, Object element)
    {
        element = JsonUtils.checkValue(element);
        
        if (list.size() <= index)
        {
            for (int i = list.size(); i<= index; i++)
                list.add(null);
        }
        
        return list.set(index, element);
    }

    @Override
    public void add(int index, Object element)
    {
        element = JsonUtils.checkValue(element);
        
        if (list.size() < index)
        {
            for (int i = list.size(); i<= index; i++)
                list.add(null);
        }
        
        list.add(index, element);
    }

    @Override
    public Object remove(int index)
    {
        return list.remove(index);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator()
    {
        return list.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index)
    {
        return list.listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex)
    {
        return new JsonArray(list.subList(fromIndex, toIndex));
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof JsonArray))
            return false;
        
        JsonArray instance = (JsonArray)o;
        return list.equals(instance.list);
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
    
    private int computeCacheSize(List<Object> list)
    {
        int cacheSize = CacheSizes.getArrayListCacheSize(list) + CacheSizes.IMMUTABLES_LIST_CACHE_SIZE;
        for (Object value : list)
            cacheSize += JsonUtils.getCacheSize(value);
        
        return cacheSize;
    }
}
