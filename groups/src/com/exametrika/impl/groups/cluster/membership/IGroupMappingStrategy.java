/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
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
     * @param nodes available worker nodes
     * @param oldGroups old groups or null if old groups are not set
     * @return new group mappings as pair of group:group delta. If group is not changed, group delta is null
     */
    List<Pair<IGroup, IGroupDelta>> mapGroups(String domain, List<INode> nodes, List<IGroup> oldGroups);
}