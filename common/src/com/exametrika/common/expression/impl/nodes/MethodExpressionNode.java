/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
 * The {@link MethodExpressionNode} is a method (expr.expr()) expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MethodExpressionNode implements IExpressionNode
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean simple;
    private final IExpressionNode methodNameExpression;
    private final List<IExpressionNode> parameterExpressions;
    private final IExpressionNode classNameExpression;
    private final boolean staticMethod;
    private volatile List<MethodInfo> methods = new ArrayList<MethodInfo>();
    
    public MethodExpressionNode(boolean simple, IExpressionNode methodNameExpression, List<IExpressionNode> parameterExpressions, 
        IExpressionNode classNameExpression, boolean staticMethod)
    {
        Assert.notNull(methodNameExpression);
        Assert.notNull(parameterExpressions);
        Assert.isTrue(classNameExpression != null == staticMethod);
        
        this.simple = simple;
        this.methodNameExpression = methodNameExpression;
        this.parameterExpressions = parameterExpressions;
        this.classNameExpression = classNameExpression;
        this.staticMethod = staticMethod;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Class clazz;
        if (staticMethod)
        {
            String className = (String)classNameExpression.evaluate(context, self);
            clazz = context.getClassResolver().resolveClass(className);
        }
        else
        {
            Assert.notNull(self);
            clazz = self.getClass();
        }
        
        String methodName = (String)methodNameExpression.evaluate(context, self);
        MethodInfo info = ensureHandle(clazz, methodName, parameterExpressions.size());
        
        Object[] parameters;
        if (staticMethod || info.isStatic)
        {
            parameters = new Object[parameterExpressions.size()];
            for (int i = 0; i < parameterExpressions.size(); i++)
            {
                Object value = parameterExpressions.get(i).evaluate(context, self);
                value = context.getConversionProvider().cast(value, info.parameterTypes[i]);
                parameters[i] = value;
            }
        }
        else
        {
            parameters = new Object[parameterExpressions.size() + 1];
            parameters[0] = self;
            for (int i = 0; i < parameterExpressions.size(); i++)
            {
                Object value = parameterExpressions.get(i).evaluate(context, self);
                value = context.getConversionProvider().cast(value, info.parameterTypes[i]);
                parameters[i + 1] = value;
            }
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
        if (staticMethod)
        {
            builder.append("@");
            builder.append(Strings.unquote(classNameExpression.toString()));
            builder.append("@.");
        }
        
        if (simple)
            builder.append(Strings.unquote(methodNameExpression.toString()));
        else
        {
            builder.append('[');
            builder.append(methodNameExpression.toString());
            builder.append(']');
        }
        
        builder.append("(");
        
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
    
    private MethodInfo ensureHandle(Class clazz, String methodName, int parametersCount)
    {
        MethodInfo info = findHandle(clazz, methodName, parametersCount);
        if (info != null)
            return info;
        else
            return addHandle(clazz, methodName, parametersCount);
    }

    private MethodInfo findHandle(Class clazz, String methodName, int parameterCount)
    {
        List<MethodInfo> methods = this.methods;
        for (int i = 0; i < methods.size(); i++)
        {
            MethodInfo info = methods.get(i);
            if (info.matches(clazz, methodName, parameterCount))
                return info;
        }
        
        return null;
    }
    
    private synchronized MethodInfo addHandle(Class clazz, String methodName, int parameterCount)
    {
        MethodInfo info = findHandle(clazz, methodName, parameterCount);
        if (info != null)
            return info;
        
        try
        {
            Method found = null;
            Class parent = clazz;
            while (parent != null && found == null)
            {
                for (Method method : parent.getDeclaredMethods())
                {
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                    if (staticMethod && !Modifier.isStatic(method.getModifiers()))
                        continue;
                    if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount)
                    {
                        found = method;
                        break;
                    }
                }
                parent = parent.getSuperclass();
            }
            
            if (found == null)
                throw new ExpressionException(messages.methodNotFound(methodName, clazz.getName()));
            
            found.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(found);
            boolean isStatic = Modifier.isStatic(found.getModifiers());
            
            info = new MethodInfo(clazz, methodName, found.getParameterTypes(), handle, isStatic);
            
            List<MethodInfo> methods = new ArrayList<MethodInfo>(this.methods);
            methods.add(info);
            
            this.methods = methods;
            return info;
        }
        catch (IllegalAccessException e)
        {
            return Exceptions.wrapAndThrow(e);
        }
    }

    private static class MethodInfo
    {
        private final Class clazz;
        private final String methodName;
        private final Class[] parameterTypes;
        private final MethodHandle handle;
        private final boolean isStatic;
        
        public MethodInfo(Class clazz, String methodName, Class[] parameterTypes, MethodHandle handle, boolean isStatic)
        {
            Assert.notNull(clazz);
            Assert.notNull(methodName);
            Assert.notNull(handle);
            
            this.clazz = clazz;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.handle = handle;
            this.isStatic = isStatic;
        }
        
        public boolean matches(Class clazz, String methodName, int parameterCount)
        {
            return this.clazz == clazz && this.methodName.equals(methodName) && this.parameterTypes.length == parameterCount;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Method ''{0}'' is not found in class ''{1}''.")
        ILocalizedMessage methodNotFound(String methodName, String className);
    }
}
