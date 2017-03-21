/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Set;

import com.exametrika.api.groups.cluster.INode;

/**
 * The {@link IWorkerNodeDiscoverer} is a discoverer of worker nodes.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IWorkerNodeDiscoverer
{
    /**
     * Takes discovered nodes.
     *
     * @return discovered nodes
     */
    Set<INode> takeDiscoveredNodes();
}