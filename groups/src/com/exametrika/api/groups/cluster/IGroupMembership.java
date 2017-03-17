/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.common.messaging.IAddress;

/**
 * The {@link IGroupMembership} is a cluster group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupMembership extends IClusterMembershipElement
{
    /**
     * Returns list of cluster groups.
     * 
     * @return list of cluster groups
     */
    List<IGroup> getGroups();
    
    /**
     * Finds group by identifier.
     * 
     * @param groupId group identifier
     * @return cluster group or <c>null</c>, if cluster group is not found
     */
    IGroup findGroup(UUID groupId);
    
    /**
     * Finds group by address.
     * 
     * @param address address of group
     * @return cluster group or <c>null</c>, if cluster group is not found
     */
    IGroup findGroup(IAddress address);
    
    /**
     * Finds list of node's groups.
     *
     * @param nodeId node identifier
     * @return list of node's groups
     */
    List<IGroup> findNodeGroups(UUID nodeId);
}