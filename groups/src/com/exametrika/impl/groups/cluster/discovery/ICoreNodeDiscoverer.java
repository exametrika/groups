/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Set;

import com.exametrika.api.groups.cluster.INode;

/**
 * The {@link ICoreNodeDiscoverer} is used to track node discovery in core group.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICoreNodeDiscoverer
{
    /**
     * Starts discovery process after local node has been completely initialized.
     */
    void startDiscovery();
    
    /**
     * Can local node form a new core group now?
     *
     * @return true if local node can form a new core group now
     */
    boolean canFormGroup();
    
    /**
     * Returns discovered nodes.
     *
     * @return discovered nodes (excluding local node)
     */
    Set<INode> getDiscoveredNodes();
}
