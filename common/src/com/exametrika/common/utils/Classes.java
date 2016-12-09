/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.InputStream;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * The {@link Classes} contains different utility methods for work with classes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Classes
{
    private static final Unsafe unsafe;
    
    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static Class forName(String name)
    {
        try
        {
            return Class.forName(name);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    /**
     * Returns most appropriate not null class loader.
     *
     * @param classLoader class loader to be used. If null, class loader is obtained from
     * current thread context class loader or current class loader
     * @return most appropriate not null class loader
     */
    public static ClassLoader getClassLoader(ClassLoader classLoader)
    {
        if (classLoader == null)
            classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null)
            classLoader = Classes.class.getClassLoader();
        
        return classLoader;
    }
    
    /**
     * Loads class by specified name.
     *
     * @param className class name
     * @param classLoader class loader to be used to load a class. Can be null if class can be loaded from
     * current thread context class loader or current class loader
     * @return class being loaded
     * @throws ClassNotFoundException
     */
    public static Class loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException
    {
        return Class.forName(className, false, getClassLoader(classLoader));
    }
    
    /**
     * Returns resource path for specified class name.
     *
     * @param clazz class name
     * @return resource path
     */
    public static String getResourcePath(Class<?> clazz)
    {
        String className = clazz.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
    
    /**
     * Returns resource input stream, loaded from class's classloader.
     *
     * @param clazz class whose classloader is used to load resource and whose package contains resource
     * @param name resource name relative to class's package
     * @return resource input stream or null if resource is not found
     */
    public static InputStream getResource(Class<?> clazz, String name)
    {
        String resourcePath = getResourcePath(clazz) + "/" + name;
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader != null)
            return classLoader.getResourceAsStream(resourcePath);
        else
            return ClassLoader.getSystemResourceAsStream(resourcePath);
    }

    public static Unsafe getUnsafe()
    {
        return unsafe;
    }
    
    /**
     * Returns package name for specified class name.
     *
     * @param className class name
     * @return package name for specified class name
     */
    public static String getPackageName(String className)
    {
        Assert.notNull(className);
        
        int pos = className.lastIndexOf('.');
        if (pos == -1)
            return className;
        
        return className.substring(0, pos);
    }
    
    private Classes()
    {
    }
}
