/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.failuredetection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;



/**
 * The {@link IFailureDetector} is a node failure detector.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFailureDetector
{
    /**
     * Returns current group coordinator.
     *
     * @return current group coordinator or null if local node is not a part of cluster
     */
    INode getCurrentCoordinator();

    /**
     * Returns list of healthy group members. First member of this list is current group coordinator.
     *
     * @return list of healthy group members
     */
    List<INode> getHealthyMembers();
    
    /**
     * Returns list of group members of current membership, whose failure has been detected.
     *
     * @return list of failed members
     */
    Set<INode> getFailedMembers();
    
    /**
     * Returns list of group members of current membership, which intentionally left the group.
     *
     * @return list of left members
     */
    Set<INode> getLeftMembers();
    
    /**
     * Adds specified group members as failed.
     *
     * @param memberIds list of identifiers of failed group members
     */
    void addFailedMembers(Set<UUID> memberIds);
    
    /**
     * Adds specified group members as left.
     *
     * @param memberIds list of identifiers of left group members
     */
    void addLeftMembers(Set<UUID> memberIds);
}
