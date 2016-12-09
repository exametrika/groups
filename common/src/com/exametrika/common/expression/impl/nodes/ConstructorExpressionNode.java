/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
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
 * The {@link ConstructorExpressionNode} is a constructor (new expr()) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ConstructorExpressionNode implements IExpressionNode
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IExpressionNode> parameterExpressions;
    private final IExpressionNode classNameExpression;
    private volatile List<ConstructorInfo> constructors = new ArrayList<ConstructorInfo>();
    
    public ConstructorExpressionNode(List<IExpressionNode> parameterExpressions, IExpressionNode classNameExpression)
    {
        Assert.notNull(parameterExpressions);
        Assert.isTrue(classNameExpression != null);
        
        this.parameterExpressions = parameterExpressions;
        this.classNameExpression = classNameExpression;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        String className = (String)classNameExpression.evaluate(context, self);
        Class clazz = context.getClassResolver().resolveClass(className);
        
        ConstructorInfo info = ensureHandle(clazz, parameterExpressions.size());
        
        Object[] parameters = new Object[parameterExpressions.size()];
        for (int i = 0; i < parameterExpressions.size(); i++)
        {
            Object value = parameterExpressions.get(i).evaluate(context, self);
            value = context.getConversionProvider().cast(value, info.parameterTypes[i]);
            parameters[i] = value;
        }
        
        try
        {
            return info.handle.invokeWithArguments(parameters);
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
        builder.append("new @");
        builder.append(Strings.unquote(classNameExpression.toString()));
        builder.append("@(");
        
        boolean first = true;
        for (IExpressionNode expression : parameterExpressions)
        {
            if (first)
                first = false;
            else
                builder.append(", ");
            
            builder.append(expression.toString());
        }
        
        builder.append(')');
        return builder.toString();
    }
    
    private ConstructorInfo ensureHandle(Class clazz, int parameterCount)
    {
        ConstructorInfo info = findHandle(clazz, parameterCount);
        if (info != null)
            return info;
        else
            return addHandle(clazz, parameterCount);
    }

    private ConstructorInfo findHandle(Class clazz, int parameterCount)
    {
        List<ConstructorInfo> constructors = this.constructors;
        for (int i = 0; i < constructors.size(); i++)
        {
            ConstructorInfo info = constructors.get(i);
            if (info.matches(clazz, parameterCount))
                return info;
        }
        
        return null;
    }
    
    private synchronized ConstructorInfo addHandle(Class clazz, int parameterCount)
    {
        ConstructorInfo info = findHandle(clazz, parameterCount);
        if (info != null)
            return info;
        
        try
        {
            Constructor found = null;
            for (Constructor constructor : clazz.getDeclaredConstructors())
            {
                if (!Modifier.isPublic(constructor.getModifiers()))
                    continue;
                if (constructor.getParameterCount() == parameterCount)
                {
                    found = constructor;
                    break;
                }
            }
            
            if (found == null)
                throw new ExpressionException(messages.constructorNotFound(parameterCount, clazz.getName()));
            
            found.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflectConstructor(found);
            
            info = new ConstructorInfo(clazz, found.getParameterTypes(), handle);
                
            List<ConstructorInfo> constructors = new ArrayList<ConstructorInfo>(this.constructors);
            constructors.add(info);
            
            this.constructors = constructors;
            return info;
        }
        catch (IllegalAccessException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }

    private static class ConstructorInfo
    {
        private final Class clazz;
        private final Class[] parameterTypes;
        private final MethodHandle handle;
        
        public ConstructorInfo(Class clazz, Class[] parameterTypes, MethodHandle handle)
        {
            Assert.notNull(clazz);
            Assert.notNull(handle);
            
            this.clazz = clazz;
            this.parameterTypes = parameterTypes;
            this.handle = handle;
        }
        
        public boolean matches(Class clazz, int parameterCount)
        {
            return this.clazz == clazz && this.parameterTypes.length == parameterCount;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Constructor with ''{0}'' parameter(s) is not found in class ''{1}''.")
        ILocalizedMessage constructorNotFound(int parameterCount, String className);
    }
}
