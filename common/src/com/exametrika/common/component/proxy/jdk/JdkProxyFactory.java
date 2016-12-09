/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy.jdk;

import java.lang.reflect.Proxy;
import java.util.List;

import com.exametrika.common.component.factory.self.AbstractSelfComponentFactory;
import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.component.proxy.IProxyFactory;


/**
 * The {@link JdkProxyFactory} is an implementation of {@link IProxyFactory} for JDK proxies.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JdkProxyFactory extends AbstractSelfComponentFactory<JdkProxyFactory> implements IProxyFactory
{
    @Override
    public <T> T createProxy(ClassLoader loader, List<Class<?>> interfaces, IInterceptor interceptor)
    {
        return (T)Proxy.newProxyInstance(loader, interfaces.toArray(new Class<?>[interfaces.size()]), new JdkInterceptor(interceptor));
    }
}
