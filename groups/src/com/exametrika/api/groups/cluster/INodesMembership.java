/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link INodesMembership} is a cluster nodes membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface INodesMembership extends IClusterMembershipElement
{
    /**
     * Returns list of cluster nodes. List is ordered from oldest to youngest node.
     * 
     * @return list of cluster nodes
     */
    List<INode> getNodes();
    
    /**
     * Finds node by identifier.
     * 
     * @param nodeId node identifier
     * @return cluster node or <c>null</c>, if cluster node is not found
     */
    INode findNode(UUID nodeId);
    
    /**
     * Finds node by address.
     * 
     * @param address address of node
     * @return cluster node or <c>null</c>, if cluster node is not found
     */
    INode findNode(IAddress address);
}