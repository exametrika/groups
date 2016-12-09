/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Strings;





/**
 * The {@link PropertyExpressionNode} is a property (expr.expr) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PropertyExpressionNode implements IExpressionNode
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean simple;
    private final IExpressionNode propertyNameExpression;
    private final IExpressionNode classNameExpression;
    private final boolean staticProperty;
    private volatile List<PropertyInfo> properties = new ArrayList<PropertyInfo>();
    
    public PropertyExpressionNode(boolean simple, IExpressionNode propertyNameExpression, IExpressionNode classNameExpression, boolean staticProperty)
    {
        Assert.notNull(propertyNameExpression);
        Assert.isTrue(classNameExpression != null == staticProperty);
        
        this.simple = simple;
        this.propertyNameExpression = propertyNameExpression;
        this.classNameExpression = classNameExpression;
        this.staticProperty = staticProperty;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object index = propertyNameExpression.evaluate(context, self).toString();
        Class clazz;
        if (staticProperty)
        {
            String className = (String)classNameExpression.evaluate(context, self);
            clazz = context.getClassResolver().resolveClass(className);
        }
        else
        {
            Assert.notNull(self);
            
            if (context.getCollectionProvider().isCollection(self, true))
                return context.getCollectionProvider().get(self, index);
            
            clazz = self.getClass();
        }
        
        String propertyName = (String)index; 
        PropertyInfo info = ensureHandle(clazz, propertyName);
        try
        {
            if (!staticProperty && !info.isStatic)
                return info.handle.invoke(self);
            else
                return info.handle.invoke();
        }
        catch (Throwable e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        if (staticProperty)
        {
            builder.append("@");
            builder.append(Strings.unquote(classNameExpression.toString()));
            builder.append("@.");
        }
        if (simple)
            builder.append(Strings.unquote(propertyNameExpression.toString()));
        else
        {
            builder.append('[');
            builder.append(propertyNameExpression.toString());
            builder.append(']');
        }
        return builder.toString();
    }
    
    private PropertyInfo ensureHandle(Class clazz, String propertyName)
    {
        PropertyInfo info = findHandle(clazz, propertyName);
        if (info != null)
            return info;
        else
            return addHandle(clazz, propertyName);
    }

    private PropertyInfo findHandle(Class clazz, String propertyName)
    {
        List<PropertyInfo> properties = this.properties;
        for (int i = 0; i < properties.size(); i++)
        {
            PropertyInfo info = properties.get(i);
            if (info.clazz == clazz && info.propertyName.equals(propertyName))
                return info;
        }
        
        return null;
    }
    
    private synchronized PropertyInfo addHandle(Class clazz, String propertyName)
    {
        PropertyInfo info = findHandle(clazz, propertyName);
        if (info != null)
            return info;
        
        MethodHandle handle;
        boolean isStatic;
        try
        {
            Field field = findField(clazz, propertyName);
            if (field != null)
            {
                field.setAccessible(true);
                handle = MethodHandles.lookup().unreflectGetter(field);
                isStatic = Modifier.isStatic(field.getModifiers());
            }
            else
            {
                Method method = findMethod(clazz, buildGetMethodName(propertyName));
                if (method != null)
                {
                    method.setAccessible(true);
                    handle = MethodHandles.lookup().unreflect(method);
                    isStatic = Modifier.isStatic(method.getModifiers());
                }
                else
                {
                    method = findMethod(clazz, buildIsMethodName(propertyName));
                    if (method != null)
                    {
                        method.setAccessible(true);
                        handle = MethodHandles.lookup().unreflect(method);
                        isStatic = Modifier.isStatic(method.getModifiers());
                    }
                    else
                        throw new ExpressionException(messages.propertyNotFound(propertyName, clazz.getName()));
                }
            }
        }
        catch (IllegalAccessException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
        
        info = new PropertyInfo(clazz, propertyName, handle, isStatic);
        List<PropertyInfo> properties = new ArrayList<PropertyInfo>(this.properties);
        properties.add(info);
        
        this.properties = properties;
        return info;
    }

    private String buildIsMethodName(String propertyName)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("is");
        builder.append(Character.toUpperCase(propertyName.charAt(0)));
        builder.append(propertyName.substring(1));
        return builder.toString();
    }

    private String buildGetMethodName(String propertyName)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("get");
        builder.append(Character.toUpperCase(propertyName.charAt(0)));
        builder.append(propertyName.substring(1));
        return builder.toString();
    }

    private Field findField(Class clazz, String fieldName)
    {
        try
        {
            Field field = clazz.getField(fieldName);
            if (staticProperty && !Modifier.isStatic(field.getModifiers()))
                return null;
            else
                return field;
        }
        catch (NoSuchFieldException e)
        {
            return null;
        }
    }
    
    private Method findMethod(Class clazz, String methodName)
    {
        try
        {
            Method method = clazz.getMethod(methodName);
            if (staticProperty && !Modifier.isStatic(method.getModifiers()))
                return null;
            else
                return method;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }
    
    private static class PropertyInfo
    {
        private final Class clazz;
        private final String propertyName;
        private final MethodHandle handle;
        private final boolean isStatic;
        
        public PropertyInfo(Class clazz, String propertyName, MethodHandle handle, boolean isStatic)
        {
            Assert.notNull(clazz);
            Assert.notNull(propertyName);
            Assert.notNull(handle);
            
            this.clazz = clazz;
            this.propertyName = propertyName;
            this.handle = handle;
            this.isStatic = isStatic;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Property ''{0}'' is not found in class ''{1}''.")
        ILocalizedMessage propertyNotFound(String propertyName, String className);
    }
}
