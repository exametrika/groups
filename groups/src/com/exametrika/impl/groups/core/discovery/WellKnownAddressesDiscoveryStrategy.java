/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.spi.groups.IDiscoveryStrategy;

/**
 * The {@link WellKnownAddressesDiscoveryStrategy} is a discovery strategy that uses wellknown addresses as cluster entry points.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class WellKnownAddressesDiscoveryStrategy implements IDiscoveryStrategy
{
    private final Set<String> wellKnownAddresses;

    /**
     * Creates a new object.
     *
     * @param wellKnownAddresses well-known addresses
     */
    public WellKnownAddressesDiscoveryStrategy(Set<String> wellKnownAddresses)
    {
        Assert.notNull(wellKnownAddresses);
        
        this.wellKnownAddresses = Immutables.wrap(wellKnownAddresses);
    }
    
    @Override
    public Set<String> getEntryPoints()
    {
        return wellKnownAddresses;
    }
}
