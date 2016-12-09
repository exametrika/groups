/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.utils.Assert;

/**
 * The {@link PercentageResourceProvider} is a resource provider that provides specified percentage of amount of resource.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PercentageResourceProvider implements IResourceProvider
{
    private final IResourceProvider resourceProvider;
    private final double percentage;

    public PercentageResourceProvider(IResourceProvider resourceProvider, double percentage)
    {
        Assert.notNull(resourceProvider);
        Assert.isTrue(percentage > 0 && percentage <= 100);
        
        this.resourceProvider = resourceProvider;
        this.percentage = percentage;
    }

    @Override
    public long getAmount()
    {
        return (long)(resourceProvider.getAmount() * percentage / 100);
    }
}
