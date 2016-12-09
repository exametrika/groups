/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

import java.lang.reflect.Field;

/**
 * The {@link Tests} implements different utility functions for testing purposes.
 * 
 * @author medvedev
 */
public class Tests
{
    /**
     * Returns a value of specified field for given object.
     *
     * @param <T> value type
     * @param o object field belongs to
     * @param fieldName field name
     * @return field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException if field is not found
     */
    public static <T> T get(Object o, String fieldName) throws IllegalAccessException, NoSuchFieldException
    {
        Class clazz = o.getClass();
        while (clazz != null)
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.getName().equals(fieldName))
                {
                    field.setAccessible(true);
                    return (T)field.get(o);
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        throw new NoSuchFieldException(fieldName);
    }
    
    /**
     * Sets a value of specified field for given object.
     *
     * @param o object field belongs to
     * @param fieldName field name
     * @param value field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException if field is not found
     */
    public static void set(Object o, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException
    {
        Class clazz = o.getClass();
        while (clazz != null)
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.getName().equals(fieldName))
                {
                    field.setAccessible(true);
                    field.set(o, value);
                    return;
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        throw new NoSuchFieldException(fieldName);
    }
    
    /**
     * Returns a value of specified static field for given class.
     *
     * @param <T> value type
     * @param clazz class field belongs to
     * @param fieldName field name
     * @return field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException if field is not found
     */
    public static <T> T get(Class clazz, String fieldName) throws IllegalAccessException, NoSuchFieldException
    {
        while (clazz != null)
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.getName().equals(fieldName))
                {
                    field.setAccessible(true);
                    return (T)field.get(null);
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        throw new NoSuchFieldException(fieldName);
    }
    
    /**
     * Returns a value of specified static field for given class.
     *
     * @param clazz class field belongs to
     * @param fieldName field name
     * @param value field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException if field is not found
     */
    public static void set(Class clazz, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException
    {
        while (clazz != null)
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.getName().equals(fieldName))
                {
                    field.setAccessible(true);
                    field.set(null, value);
                    return;
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        throw new NoSuchFieldException(fieldName);
    }
}
