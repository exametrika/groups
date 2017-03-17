/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;


/**
 * The {@link IGroupDelta} represents a difference between old and new group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupDelta
{
    /**
     * Returns group identifier.
     *
     * @return group identifier
     */
    UUID getId();
    
    /**
     * Is group primary?
     *
     * @return true if group is primary
     */
    boolean isPrimary();
    
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