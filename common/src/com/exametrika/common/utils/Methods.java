/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * The {@link Methods} contains different utility methods for work with methods.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Methods
{
    public static class MethodBuilder
    {
        private Object instance;

        public MethodBuilder(Object instance)
        {
            this.instance = instance;
        }
        
        public InvokeBuilder method(String methodName)
        {
            return new InvokeBuilder(this, methodName, instance);
        }
        
        public MethodBuilder invoke(String methodName)
        {
            Assert.notNull(methodName);
            
            if (instance != null)
                instance = Methods.invoke(instance, methodName);
            return this;
        }
        
        public <T> T toResult()
        {
            return (T)instance;
        }
    }
    
    public static class InvokeBuilder
    {
        private final MethodBuilder parent;
        private final Object instance;
        private List<Class> parameterTypes = new ArrayList<Class>();
        private List<Object> parameters = new ArrayList<Object>();
        private final String methodName;
        
        public InvokeBuilder(MethodBuilder parent, String methodName, Object instance)
        {
            Assert.notNull(parent);
            Assert.notNull(methodName);
            
            this.parent = parent;
            this.methodName = methodName;
            this.instance = instance;
        }
        
        public InvokeBuilder param(Class clazz, Object value)
        {
            Assert.notNull(clazz);
            
            parameterTypes.add(clazz);
            parameters.add(value);
            return this;
        }
        
        public MethodBuilder invoke()
        {
            if (instance == null)
                return parent;
            
            Method method = Methods.getMethod(instance, methodName, parameterTypes.toArray(new Class[parameterTypes.size()]));
            
            try
            {
                parent.instance = method.invoke(instance, parameters.toArray());
                return parent;
            }
            catch (Exception e)
            {
                return Exceptions.wrapAndThrow(e);
            }
        }
    }
    
    public static MethodBuilder builder(Object instance)
    {
        return new MethodBuilder(instance);
    }
    
    public static Method getMethod(Object instance, String methodName, Class<?>... parameterTypes)
    {
        Assert.notNull(instance);
        Assert.notNull(methodName);
        Assert.notNull(parameterTypes);
        
        try
        {
            return instance.getClass().getMethod(methodName, parameterTypes);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    public static <T> T invoke(Object instance, String methodName)
    {
        Assert.notNull(instance);
        Assert.notNull(methodName);
        
        try
        {
            Method method = instance.getClass().getMethod(methodName);
            return (T)method.invoke(instance);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    public static Constructor getConstructor(Class<?> clazz, Class<?>... parameterTypes)
    {
        Assert.notNull(clazz);
        Assert.notNull(parameterTypes);
        
        try
        {
            return clazz.getConstructor(parameterTypes);
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    public static <T> T newInstance(Class<?> clazz)
    {
        Assert.notNull(clazz);
        
        try
        {
            Constructor constructor = clazz.getConstructor();
            return (T)constructor.newInstance();
        }
        catch (Exception e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    private Methods()
    {
    }
}
