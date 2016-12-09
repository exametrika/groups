/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy;

import java.util.List;


/**
 * The {@link IProxyFactory} is a factory for proxy instances.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IProxyFactory
{
    /**
     * Creates a proxy instance.
     *
     * @param <T> proxy type
     * @param loader class loader of proxy class
     * @param interfaces list of interfaces proxy must implement
     * @param interceptor call interceptor
     * @return proxy instance
     */
    <T> T createProxy(ClassLoader loader, List<Class<?>> interfaces, IInterceptor interceptor);
}
