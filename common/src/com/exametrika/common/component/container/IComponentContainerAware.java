/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;



/**
 * The {@link IComponentContainerAware} is a helper interface to inject {@link IComponentContainer} instance.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * 
 * @author Medvedev-A
 */
public interface IComponentContainerAware
{
    /**
     * Injects {@link IComponentContainer} instance.
     *
     * @param container component container
     */
    void setContainer(IComponentContainer container);
}
