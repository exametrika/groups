/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.Set;

import com.exametrika.api.groups.core.INode;


/**
 * The {@link INodeMembershipChange} represents a difference between old and new node membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface INodeMembershipChange extends IClusterMembershipElementChange
{
    /**
     * Returns set of new cluster
     * 
     * @return set of new cluster
     */
    Set<INode> getJoinedNodes();
    
    /**
     * Returns set of left cluster nodes.
     * 
     * @return set of left custer nodes
     */
    Set<INode> getLeftNodes();
    
    /**
     * Returns set of failed cluster nodes.
     * 
     * @return set of failed cluster nodes
     */
    Set<INode> getFailedNodes();
}