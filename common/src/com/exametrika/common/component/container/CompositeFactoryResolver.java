/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositeFactoryResolver} is an implementation of {@link IFactoryResolver} that delegates
 * component factory resolution to specified list of resolvers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositeFactoryResolver implements IFactoryResolver
{
    private final List<? extends IFactoryResolver> resolvers;

    /**
     * Creates an object.
     *
     * @param resolvers list of component factory resolvers
     */
    public CompositeFactoryResolver(List<? extends IFactoryResolver> resolvers)
    {
        Assert.notNull(resolvers);
        
        this.resolvers = resolvers;
    }

    @Override
    public <T> IComponentFactory<T> resolveFactory(String componentName, Map<String, ?> qualifiers)
    {
        Assert.notNull(componentName);
        
        for (IFactoryResolver resolver : resolvers)
        {
            IComponentFactory<T> factory = resolver.resolveFactory(componentName, qualifiers);
            if (factory != null)
                return factory;
        }
        return null;
    }
}
