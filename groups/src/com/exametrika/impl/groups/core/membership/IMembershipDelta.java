/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;


/**
 * The {@link IMembershipDelta} represents a difference between old and new membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IMembershipDelta
{
    /**
     * Returns identifier of new membership.
     *
     * @return identifier of new membership
     */
    long getId();
    
    /**
     * Returns list of new group members
     * 
     * @return list of new group members
     */
    List<INode> getJoinedMembers();
    
    /**
     * Returns set of left group members.
     * 
     * @return set of left group members
     */
    Set<UUID> getLeftMembers();
    
    /**
     * Returns set of failed group members.
     * 
     * @return set of failed group members
     */
    Set<UUID> getFailedMembers();
}