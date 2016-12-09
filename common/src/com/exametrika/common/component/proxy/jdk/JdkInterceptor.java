/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JdkInterceptor} is an implementation of {@link IInterceptor} for JDK proxies.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JdkInterceptor implements InvocationHandler
{
    private final IInterceptor interceptor;

    /**
     * Creates a new object.
     *
     * @param interceptor interceptor
     */
    public JdkInterceptor(IInterceptor interceptor)
    {
        Assert.notNull(interceptor);
        
        this.interceptor = interceptor;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        return interceptor.invoke(new JdkInvocation(new JdkInvoker(), proxy, method, args, null));
    }
}
