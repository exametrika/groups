/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.expression.IClassResolver;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;







/**
 * The {@link StandardClassResolver} is a standard class resolver.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StandardClassResolver implements IClassResolver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<String> packages;
    private final Map<String, String> classes;
    private volatile Map<String, Class> resolvedClasses;
    
    public StandardClassResolver()
    {
        this(Collections.<String>emptyList(), Collections.<String, String>emptyMap());
    }
    
    public StandardClassResolver(List<String> packages, Map<String, String> classes)
    {
        Assert.notNull(packages);
        Assert.notNull(classes);

        this.packages = new ArrayList(packages);
        this.classes = new HashMap<String, String>(classes);
        
        resolvedClasses = addDefaultClasses();
    }

    public synchronized void addPackage(String packageName)
    {
        Assert.notNull(packageName);
        
        packages.add(packageName);
    }
    
    public synchronized void addClass(String alias, String className)
    {
        Assert.notNull(alias);
        Assert.notNull(className);
        
        String prev = classes.put(alias, className);
        Assert.isNull(prev);
    }
    
    public synchronized void addResolvedClass(String alias, Class clazz)
    {
        Assert.notNull(alias);
        Assert.notNull(clazz);
        
        Map<String, Class> resolvedClasses = new HashMap<String, Class>(this.resolvedClasses);
        Class prev = resolvedClasses.put(alias, clazz);
        Assert.isNull(prev);
        
        this.resolvedClasses = resolvedClasses;
    }
    
    @Override
    public Class resolveClass(String className)
    {
        Assert.notNull(className);
        
        Map<String, Class> resolvedClasses = this.resolvedClasses;
        Class clazz = resolvedClasses.get(className);
        if (clazz == null)
            clazz = loadClass(className);
        
        return clazz;
    }
    
    private synchronized Class loadClass(String className)
    {
        Assert.notNull(className);
        
        Class clazz = resolvedClasses.get(className);
        if (clazz == null)
        {
            if (classes.containsKey(className))
                className = classes.get(className);
            
            clazz = findClass(className);
            if (clazz == null)
            {
                for (String packageName : packages)
                {
                    clazz = findClass(packageName + "." + className);
                    if (clazz != null)
                        break;
                }
                
                if (clazz == null)
                    throw new ExpressionException(messages.classNotFound(className));
            }
            
            Map<String, Class> resolvedClasses = new HashMap<String, Class>(this.resolvedClasses);
            resolvedClasses.put(className, clazz);
            this.resolvedClasses = resolvedClasses;
        }
        
        return clazz;
    }
    
    private Class findClass(String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
    }
    
    private static Map<String, Class> addDefaultClasses()
    {
        Map<String, Class> resolvedClasses = new HashMap<String, Class>();
        
        resolvedClasses.put("s", String.class);
        resolvedClasses.put("string", String.class);
        resolvedClasses.put("String", String.class);
        resolvedClasses.put(String.class.getName(), String.class);

        resolvedClasses.put("i", Integer.class);
        resolvedClasses.put("int", Integer.class);
        resolvedClasses.put("Integer", Integer.class);
        resolvedClasses.put(Integer.class.getName(), Integer.class);
        
        resolvedClasses.put("l", Long.class);
        resolvedClasses.put("long", Long.class);
        resolvedClasses.put("Long", Long.class);
        resolvedClasses.put(Long.class.getName(), Long.class);
        
        resolvedClasses.put("f", Float.class);
        resolvedClasses.put("float", Float.class);
        resolvedClasses.put("Float", Float.class);
        resolvedClasses.put(Float.class.getName(), Float.class);
        
        resolvedClasses.put("d", Double.class);
        resolvedClasses.put("double", Double.class);
        resolvedClasses.put("Double", Double.class);
        resolvedClasses.put(Double.class.getName(), Double.class);
        
        resolvedClasses.put("b", Boolean.class);
        resolvedClasses.put("boolean", Boolean.class);
        resolvedClasses.put("Boolean", Boolean.class);
        resolvedClasses.put(Boolean.class.getName(), Boolean.class);
        
        resolvedClasses.put("date", Date.class);
        resolvedClasses.put("Date", Date.class);
        resolvedClasses.put(Date.class.getName(), Date.class);
        
        return resolvedClasses;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Class ''{0}'' is not found.")
        ILocalizedMessage classNotFound(String className);
    }
}
