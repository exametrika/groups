/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Set;

import com.exametrika.api.groups.cluster.INode;

/**
 * The {@link IGroupNodeDiscoverer} is used to track node discovery in worker group.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupNodeDiscoverer
{
    /**
     * Returns discovered nodes.
     *
     * @return discovered nodes (excluding local node)
     */
    Set<INode> getDiscoveredNodes();
}
