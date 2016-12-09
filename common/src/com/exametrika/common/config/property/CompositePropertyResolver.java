/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;

import java.util.List;

import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositePropertyResolver} is an implementation of {@link IPropertyResolver} that delegates
 * property resolution to specified list of resolvers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositePropertyResolver implements IPropertyResolver
{
    private final List<? extends IPropertyResolver> resolvers;

    /**
     * Creates an object.
     *
     * @param resolvers list of property resolvers
     */
    public CompositePropertyResolver(List<? extends IPropertyResolver> resolvers)
    {
        Assert.notNull(resolvers);
        
        this.resolvers = resolvers;
    }

    @Override
    public String resolveProperty(String propertyName)
    {
        Assert.notNull(propertyName);
        
        for (IPropertyResolver resolver : resolvers)
        {
            String propertyValue = resolver.resolveProperty(propertyName);
            if (propertyValue != null)
                return propertyValue;
        }
        return null;
    }
}
