/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.discovery;

import java.util.Set;


/**
 * The {@link IDiscoveryStrategy} defines specific strategy for discovery of cluster entry points.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IDiscoveryStrategy
{
    /**
     * Returns connection addresses of cluster entry points.
     *
     * @return connection addresses of cluster entry points
     */
    Set<String> getEntryPoints();
}
