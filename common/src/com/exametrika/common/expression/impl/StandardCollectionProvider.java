/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.ICollectionProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link StandardCollectionProvider} is a standard collection provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StandardCollectionProvider implements ICollectionProvider
{
    @Override
    public boolean isCollection(Object collection, boolean get)
    {
        if (get && collection instanceof String)
            return true;
        else
            return collection instanceof Map || collection instanceof List || collection instanceof Object[] || 
                collection.getClass().isArray();
    }
    
    @Override
    public Object get(Object collection, Object index)
    {
        Assert.notNull(collection);
        Assert.notNull(index);
        
        if (collection instanceof Map)
            return ((Map)collection).get(index);
        else
        {
            int intIndex;
            if (index instanceof Number)
                intIndex = ((Number)index).intValue();
            else 
                intIndex = Integer.parseInt(index.toString());
            
            if (collection instanceof List)
                return ((List)collection).get(intIndex);
            else if (collection instanceof Object[])
                return ((Object[])collection)[intIndex];
            else if (collection.getClass().isArray())
                return Array.get(collection, intIndex);
            else if (collection instanceof String)
                return ((String)collection).charAt(intIndex);
            else
                return Assert.error();
        }
    }
    
    @Override
    public void set(Object collection, Object index, Object value)
    {
        Assert.notNull(collection);
        Assert.notNull(index);
        
        if (collection instanceof Map)
            ((Map)collection).put(index, value);
        else
        {
            int intIndex;
            if (index instanceof Number)
                intIndex = ((Number)index).intValue();
            else 
                intIndex = Integer.parseInt(index.toString());
            
            if (collection instanceof ArrayList)
                Collections.set((ArrayList)collection, intIndex, value);
            else if (collection instanceof List)
                ((List)collection).set(intIndex, value);
            else if (collection instanceof Object[])
                ((Object[])collection)[intIndex] = value;
            else if (collection.getClass().isArray())
                Array.set(collection, intIndex, value);
            else
                Assert.error();
        }
    }

    @Override
    public Iterable getIterable(Object collection)
    {
        Assert.notNull(collection);
        
        if (collection instanceof Map)
            return ((Map)collection).entrySet();
        else if (collection instanceof Iterable)
            return (Iterable)collection;
        else if (collection instanceof Object[])
            return Arrays.asList((Object[])collection);
        else if (collection.getClass().isArray())
            return new ArrayIterable(collection);
        else if (collection instanceof String)
            return new StringIterable((String)collection);
        else
            return Assert.error();
    }
    
    @Override
    public boolean contains(Object collection, Object value)
    {
        Assert.notNull(collection);
        
        if (collection instanceof Map)
            return ((Map)collection).containsKey(value);
        else if (collection instanceof Collection)
            return ((Collection)collection).contains(value);
        else if (collection instanceof Object[])
            return Arrays.asList((Object[])collection).contains(value);
        else if (collection.getClass().isArray())
        {
            int count = Array.getLength(collection);
            for (int i = 0; i < count; i++)
            {
                Object element = Array.get(collection, i);
                if (element.equals(value))
                    return true;
            }
            return false;
        }
        else if (collection instanceof String)
        {
            if (value == null)
                return false;
            
            return ((String)collection).indexOf(value.toString()) != -1;
        }
        else
            return Assert.error();
    }
    
    private static class ArrayIterable implements Iterable
    {
        private Object collection;
        
        public ArrayIterable(Object collection)
        {
            this.collection = collection;
        }
        
        @Override
        public Iterator iterator()
        {
            return new ArrayIterator(collection);
        }
    }
    
    private static class ArrayIterator implements Iterator
    {
        private final Object collection;
        private final int count;
        private int index;
        
        public ArrayIterator(Object collection)
        {
            this.collection = collection;
            this.count = Array.getLength(collection);
        }

        @Override
        public boolean hasNext()
        {
            return index < count;
        }

        @Override
        public Object next()
        {
            Object res = Array.get(collection, index);
            index++;
            return res;
        }
    }
    
    private static class StringIterable implements Iterable
    {
        private String collection;
        
        public StringIterable(String collection)
        {
            this.collection = collection;
        }
        
        @Override
        public Iterator iterator()
        {
            return new StringIterator(collection);
        }
    }
    
    private static class StringIterator implements Iterator
    {
        private final String collection;
        private int index;
        
        public StringIterator(String collection)
        {
            this.collection = collection;
        }

        @Override
        public boolean hasNext()
        {
            return index < collection.length();
        }

        @Override
        public Object next()
        {
            Object res = collection.charAt(index);
            index++;
            return res;
        }
    }

}
