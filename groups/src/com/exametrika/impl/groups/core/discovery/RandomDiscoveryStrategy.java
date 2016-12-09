/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.IDiscoveryStrategy;

/**
 * The {@link RandomDiscoveryStrategy} is a discovery strategy that select several randomly chosen addresses as cluster entry points.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class RandomDiscoveryStrategy implements IDiscoveryStrategy
{
    private final int selectCount;
    private final Random random = new Random();
    private final IDiscoveryStrategy discoveryStrategy;

    /**
     * Creates a new object.
     *
     * @param selectCount number of selected addresses
     * @param discoveryStrategy base discovery strategy
     */
    public RandomDiscoveryStrategy(int selectCount, IDiscoveryStrategy discoveryStrategy)
    {
        Assert.notNull(discoveryStrategy);
        
        this.selectCount = selectCount;
        this.discoveryStrategy = discoveryStrategy;
    }
    
    @Override
    public Set<String> getEntryPoints()
    {
        List<String> entryPoints = new ArrayList<String>(discoveryStrategy.getEntryPoints());
        
        if (selectCount >= entryPoints.size())
            return new HashSet<String>(entryPoints);
        
        Set<String> selectedAddresses = new HashSet<String>();
        for (int i = 0; i < selectCount; i++)
        {
            int k = random.nextInt(entryPoints.size());
            selectedAddresses.add(entryPoints.remove(k));
        }
        
        return selectedAddresses;
    }
}
