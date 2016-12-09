/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy.jdk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.exametrika.common.component.proxy.IInvocation;
import com.exametrika.common.component.proxy.InvocationException;


/**
 * The {@link JdkInvoker} is an invoker of objects for JDK proxies.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JdkInvoker
{
    public Object invoke(IInvocation invocation)
    {
        JdkInvocation jdkInvocation = (JdkInvocation)invocation;
        if (jdkInvocation.getTarget() != null)
        {
            Object target = jdkInvocation.getTarget();
            Object[] args = jdkInvocation.getArguments();
            Method method = jdkInvocation.getMethod();
            
            try
            {
                return method.invoke(target, args);
            }
            catch (InvocationTargetException e)
            {
                throw new InvocationException(e.getCause());
            }
            catch (IllegalAccessException e)
            {
                throw new InvocationException(e);
            }
        }

        return null;
    }
}
