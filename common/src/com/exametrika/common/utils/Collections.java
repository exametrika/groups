/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link Collections} contains different utility methods for collection manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Collections
{
    /**
     * Is collection empty or null?
     *
     * @param value value
     * @return true if string is empty or null
     */
    public static boolean isEmpty(Collection value)
    {
        if (value == null || value.isEmpty())
            return true;
        else
            return false;
    }
    
    /**
     * Returns non-null value.
     *
     * @param value value
     * @return value
     */
    public static <T extends List> T notNull(T value)
    {
        if (value == null)
            return (T)java.util.Collections.emptyList();
        else
            return value;
    }
    
    /**
     * Returns non-null value.
     *
     * @param value value
     * @return value
     */
    public static <T extends Set> T notNull(T value)
    {
        if (value == null)
            return (T)java.util.Collections.emptySet();
        else
            return value;
    }
    
    /**
     * Returns non-null value.
     *
     * @param value value
     * @return value
     */
    public static <T extends Map> T notNull(T value)
    {
        if (value == null)
            return (T)java.util.Collections.emptyMap();
        else
            return value;
    }
    
    /**
     * Combines two arrays into larger one.
     *
     * @param <T> array element type
     * @param array1 first array. Can be <c>null<c>
     * @param array2 second array. Can be <c>null<c>
     * @return combined array, or <c>null<c> if both arrays are nulls
     */
    public static <T> T[] combine(T[] array1, T[] array2)
    {
        if (array1 == null)
            return array2;
        else if (array2 == null)
            return array1;
        else
        {
            T[] array = (T[])Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
            System.arraycopy(array1, 0, array, 0, array1.length);
            System.arraycopy(array2, 0, array, array1.length, array2.length);
            
            return array;
        }
    }
    
    /**
     * Create list with not null elements specified.
     *
     * @param <T> list element type
     * @param elements list of elements
     * @return list of not null elements
     */
    public static <T> List<T> getNotNullList(T ... elements)
    {
        List<T> list = new ArrayList<T>();
        for (T element : elements)
        {
            if (element != null)
                list.add(element);
        }
        
        return list;
    }
    
    /**
     * Create set with not null elements specified.
     *
     * @param <T> set element type
     * @param elements list of elements
     * @return set of not null elements
     */
    public static <T> Set<T> getNotNullSet(T ... elements)
    {
        Set<T> list = new HashSet<T>();
        for (T element : elements)
        {
            if (element != null)
                list.add(element);
        }
        
        return list;
    }
    
    /**
     * Converts array to list.
     *
     * @param <T> element type
     * @param array array. Can be null
     * @return list
     */
    public static <T> List<T> toList(T[] array)
    {
        ArrayList<T> list = new ArrayList<T>();
        
        if (array != null)
        {
            for (T element : array)
                list.add(element);
        }
        
        return list;
    }
    
    /**
     * Converts iterator to list.
     *
     * @param <T> element type
     * @param it iterator. Can be null
     * @return list
     */
    public static <T> List<T> toList(Iterator<T> it)
    {
        ArrayList<T> list = new ArrayList<T>();
        
        while (it.hasNext())
            list.add(it.next());
        
        return list;
    }
    
    /**
     * Creates {@link Set} from list of arguments.
     *
     * @param <T> element type
     * @param args arguments list
     * @return set
     */
    public static <T> Set<T> asSet(T ... args)
    {
        Set<T> set = new LinkedHashSet<T>();
        
        for (T arg : args)
            set.add(arg);
        
        return set;
    }
    
    public static <T> T get(List<T> list, int index)
    {
        if (index < list.size())
            return list.get(index);
        else
            return null;
    }
    
    public static <T> void set(ArrayList<T> list, int index, T element)
    {
        if (index >= list.size())
        {
            list.ensureCapacity(index + 1);
            for (int i = list.size(); i <= index; i++)
                list.add(null);
        }
        
        list.set(index, element);
    }

    private Collections()
    {
    }
}
