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
 * The {@link PropertyAssignmentExpressionNode} is a property assignment (expr.expr = expr) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PropertyAssignmentExpressionNode implements IExpressionNode
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean simple;
    private final IExpressionNode propertyNameExpression;
    private final IExpressionNode classNameExpression;
    private final boolean staticProperty;
    private final IExpressionNode expression;
    private volatile List<PropertyInfo> properties = new ArrayList<PropertyInfo>();
    
    public PropertyAssignmentExpressionNode(boolean simple, IExpressionNode propertyNameExpression, IExpressionNode classNameExpression, 
        boolean staticProperty, IExpressionNode expression)
    {
        Assert.notNull(propertyNameExpression);
        Assert.isTrue(classNameExpression != null == staticProperty);
        Assert.notNull(expression);
        
        this.simple = simple;
        this.propertyNameExpression = propertyNameExpression;
        this.classNameExpression = classNameExpression;
        this.staticProperty = staticProperty;
        this.expression = expression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object index = propertyNameExpression.evaluate(context, self).toString();
        Object value = expression.evaluate(context, self);
        Class clazz;
        if (staticProperty)
        {
            String className = (String)classNameExpression.evaluate(context, self);
            clazz = context.getClassResolver().resolveClass(className);
        }
        else
        {
            Assert.notNull(self);
            
            if (context.getCollectionProvider().isCollection(self, false))
            {
                context.getCollectionProvider().set(self, index, value);
                return value;
            }
            
            clazz = self.getClass();
        }
        
        String propertyName = (String)index; 
        PropertyInfo info = ensureHandle(clazz, propertyName);
        try
        {
            value = context.getConversionProvider().cast(value, info.type);
            
            if (!staticProperty && !info.isStatic)
                info.handle.invoke(self, value);
            else
                info.handle.invoke(value);
            
            return value;
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
        
        builder.append(" = ");
        builder.append(expression.toString());
        
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
        Class type;
        boolean isStatic;
        try
        {
            Field field = findField(clazz, propertyName);
            if (field != null)
            {
                field.setAccessible(true);
                type = field.getType();
                handle = MethodHandles.lookup().unreflectSetter(field);
                isStatic = Modifier.isStatic(field.getModifiers());
            }
            else
            {
                Method method = findMethod(clazz, buildSetMethodName(propertyName));
                if (method != null)
                {
                    method.setAccessible(true);
                    type = method.getParameterTypes()[0];
                    handle = MethodHandles.lookup().unreflect(method);
                    isStatic = Modifier.isStatic(method.getModifiers());
                }
                else
                    throw new ExpressionException(messages.propertyNotFound(propertyName, clazz.getName()));
            }
        }
        catch (IllegalAccessException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
        
        info = new PropertyInfo(clazz, propertyName, type, handle, isStatic);
        List<PropertyInfo> properties = new ArrayList<PropertyInfo>(this.properties);
        properties.add(info);
        
        this.properties = properties;
        return info;
    }

    private String buildSetMethodName(String propertyName)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("set");
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
        Method found = null;
        Class parent = clazz;
        while (parent != null && found == null)
        {
            for (Method method : parent.getDeclaredMethods())
            {
                if (!Modifier.isPublic(method.getModifiers()))
                    continue;
                if (staticProperty && !Modifier.isStatic(method.getModifiers()))
                    continue;
                if (method.getName().equals(methodName) && method.getParameterCount() == 1)
                {
                    found = method;
                    break;
                }
            }
            parent = parent.getSuperclass();
        }
        
        return found;
    }
    
    private static class PropertyInfo
    {
        private final Class clazz;
        private final String propertyName;
        private final Class type;
        private final MethodHandle handle;
        private final boolean isStatic;
        
        public PropertyInfo(Class clazz, String propertyName, Class type, MethodHandle handle, boolean isStatic)
        {
            Assert.notNull(clazz);
            Assert.notNull(propertyName);
            Assert.notNull(type);
            Assert.notNull(handle);
            
            this.clazz = clazz;
            this.propertyName = propertyName;
            this.type = type;
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
