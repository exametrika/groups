/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.proxy;

/**
 * The {@link IInvocation} is a helper object that holds invocation parameters.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IInvocation
{
    /**
     * Returns a call arguments.
     *
     * @return call arguments
     */
    Object[] getArguments();
    
    /**
     * Returns a proxy instance being intercepted.
     *
     * @param <T> proxy type name
     * @return proxy being intercepted
     */
    <T> T getThis();
    
    /**
     * Returns target of invocation.
     *
     * @param <T> target type name
     * @return invocation target 
     */
    <T> T getTarget();
    
    /**
     * Sets target of invocation.
     *
     * @param target invocation target. Can be <c>null<c>
     */
    void setTarget(Object target);
    
    /**
     * Proceeds call after interception.
     *
     * @param <T> return type name
     * @return call return value
     */
    <T> T proceed();
}
