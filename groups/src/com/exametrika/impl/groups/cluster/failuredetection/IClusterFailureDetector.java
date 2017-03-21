/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.Set;

import com.exametrika.api.groups.cluster.INode;



/**
 * The {@link IClusterFailureDetector} is a cluster node failure detector.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IClusterFailureDetector
{
    /**
     * Takes list of cluster nodes, whose failure has been detected.
     *
     * @return list of failed nodes
     */
    Set<INode> takeFailedNodes();
    
    /**
     * Returns list of cluster nodes, which intentionally left the group.
     *
     * @return list of left nodes
     */
    Set<INode> takeLeftNodes();
}
