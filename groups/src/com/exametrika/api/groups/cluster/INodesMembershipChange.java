/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;
import java.util.Set;


/**
 * The {@link INodesMembershipChange} represents a difference between old and new node membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface INodesMembershipChange extends IClusterMembershipElementChange
{
    /**
     * Returns set of new cluster nodes
     * 
     * @return set of new cluster nodes
     */
    List<INode> getJoinedNodes();
    
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