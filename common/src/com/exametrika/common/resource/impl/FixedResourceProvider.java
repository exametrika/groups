/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.utils.Assert;

/**
 * The {@link FixedResourceProvider} is a resource provider that provides fixed amount of resource.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FixedResourceProvider implements IResourceProvider
{
    private final long amount;

    public FixedResourceProvider(long amount)
    {
        Assert.isTrue(amount > 0);
        this.amount = amount;
    }
    
    @Override
    public long getAmount()
    {
        return amount;
    }
}
