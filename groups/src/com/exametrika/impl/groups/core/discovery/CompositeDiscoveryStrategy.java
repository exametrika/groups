/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.IDiscoveryStrategy;

/**
 * The {@link CompositeDiscoveryStrategy} is a composite discovery strategy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class CompositeDiscoveryStrategy implements IDiscoveryStrategy
{
    private final List<IDiscoveryStrategy> discoveryStrategies;

    /**
     * Creates a new object.
     *
     * @param discoveryStrategies discovery strategies
     */
    public CompositeDiscoveryStrategy(List<IDiscoveryStrategy> discoveryStrategies)
    {
        Assert.notNull(discoveryStrategies);
        
        this.discoveryStrategies = discoveryStrategies;
    }
    
    @Override
    public Set<String> getEntryPoints()
    {
        Set<String> entryPoints = new HashSet<String>();
        
        for (IDiscoveryStrategy discoveryStrategy : discoveryStrategies)
            entryPoints.addAll(discoveryStrategy.getEntryPoints());
        
        return entryPoints;
    }
}
