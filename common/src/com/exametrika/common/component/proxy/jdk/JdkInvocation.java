/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy.jdk;

import java.lang.reflect.Method;

import com.exametrika.common.component.proxy.IInvocation;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JdkInvocation} is an implementation of {@link IInvocation} for JDK proxies.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class JdkInvocation implements IInvocation
{
    private final JdkInvoker invoker;
    private final Object proxy;
    private final Method method;
    private final Object[] args;
    private Object target;

    /**
     * Creates a new object.
     *
     * @param invoker invoker
     * @param proxy proxy
     * @param method method
     * @param args arguments
     * @param target target. Can be null
     */
    public JdkInvocation(JdkInvoker invoker, Object proxy, Method method, Object[] args, Object target)
    {
        Assert.notNull(invoker);
        Assert.notNull(proxy);
        Assert.notNull(method);
        
        this.invoker = invoker;
        this.proxy = proxy;
        this.target = target;
        this.method = method;
        this.args = args;
    }
    
    @Override
    public Object[] getArguments()
    {
        return args;
    }

    @Override
    public <T> T getThis()
    {
        return (T)proxy;
    }

    @Override
    public <T> T getTarget()
    {
        return (T)target;
    }

    @Override
    public void setTarget(Object target)
    {
        this.target = target; 
    }
    
    @Override
    public <T> T proceed()
    {
        return (T)invoker.invoke(this);
    }
    
    public Method getMethod()
    {
        return method;
    }
}
