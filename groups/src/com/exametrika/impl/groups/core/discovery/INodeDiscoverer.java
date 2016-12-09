/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.Set;

import com.exametrika.api.groups.core.INode;

/**
 * The {@link INodeDiscoverer} is used to track node discovery.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface INodeDiscoverer
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
     * @return discovered nodes
     */
    Set<INode> getDiscoveredNodes();
}
