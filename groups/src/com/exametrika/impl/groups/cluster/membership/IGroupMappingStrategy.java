/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.common.utils.Pair;

/**
 * The {@link IGroupMappingStrategy} represents a group mapping strategy.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupMappingStrategy
{
    /**
     * Maps worker nodes to groups.
     *
     * @param domain domain
     * @param nodeMembership new worker nodes membership
     * @param nodesMembershipDelta new worker nodes membership delta
     * @param oldGroupMembership old groups membership or null if old groups membership is not set
     * @return new group mappings as pair of group:<group delta>. If group is not changed, group delta is null.
     *         If all groups are not changed returns null
     */
    List<Pair<IGroup, IGroupDelta>> mapGroups(String domain, NodesMembership nodeMembership, NodesMembershipDelta nodesMembershipDelta, 
        GroupsMembership oldGroupMembership);
}