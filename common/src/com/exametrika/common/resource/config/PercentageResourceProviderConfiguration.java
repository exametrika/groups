/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.PercentageResourceProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link PercentageResourceProviderConfiguration} is a configuration of percentage resource provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PercentageResourceProviderConfiguration extends ResourceProviderConfiguration
{
    private final ResourceProviderConfiguration resourceProvider;
    private final double percentage;

    public PercentageResourceProviderConfiguration(ResourceProviderConfiguration resourceProvider, double percentage)
    {
        Assert.notNull(resourceProvider);
        Assert.isTrue(percentage >= 0 && percentage <= 100);
        
        this.resourceProvider = resourceProvider;
        this.percentage = percentage;
    }
    
    public ResourceProviderConfiguration getResourceProvider()
    {
        return resourceProvider;
    }

    public double getPercentage()
    {
        return percentage;
    }

    @Override
    public PercentageResourceProvider createProvider()
    {
        return new PercentageResourceProvider(resourceProvider.createProvider(), percentage);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof PercentageResourceProviderConfiguration))
            return false;
        
        PercentageResourceProviderConfiguration configuration = (PercentageResourceProviderConfiguration)o;
        return resourceProvider.equals(configuration.resourceProvider) && percentage == configuration.percentage;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(resourceProvider, percentage);
    }
}
