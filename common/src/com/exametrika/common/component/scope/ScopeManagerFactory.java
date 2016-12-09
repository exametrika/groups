/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;

/**
 * The {@link ScopeManagerFactory} is a factory for {@link ScopeManager}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ScopeManagerFactory extends AbstractSingletonComponentFactory<ScopeManager>
{
    public ScopeManagerFactory()
    {
        super(true);
    }
    
    @Override
    protected ScopeManager createInstance()
    {
        return new ScopeManager();
    }
}
