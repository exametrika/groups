/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.module;

import com.exametrika.common.component.container.IComponentContainer;

/**
 * The {@link IModule} is a module, i.e. unit of deployment of component application.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IModule
{
    /**
     * Returns module name.
     *
     * @return module name
     */
    String getName();
    
    /**
     * Returns module's component container.
     *
     * @return module's component container
     */
    IComponentContainer getContainer();
}
