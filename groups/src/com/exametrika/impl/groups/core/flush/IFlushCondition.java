/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;

/**
 * The {@link IFlushCondition} is a flush starting condition.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlushCondition
{
    /**
     * Can flush be started now?
     *
     * @param members new group members
     * @param discoveredMembers discovered members
     * @param failedMemberIds failed members identifiers
     * @param leftMemberIds left members identifiers
     * @return true if flush can be started now with given posdibly modified set of discovered, failed and left nodes
     */
    boolean canStartFlush(List<INode> members, List<INode> discoveredMembers, Set<UUID> failedMemberIds, Set<UUID> leftMemberIds);
}
