/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.Set;


/**
 * The {@link IGroupChange} represents a difference between old and new group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupChange
{
    /**
     * Returns new group membership.
     *
     * @return new group membership
     */
    IGroup getNewGroup();
    
    /**
     * Returns old group membership.
     *
     * @return old group membership
     */
    IGroup getOldGroup();
    
    /**
     * Returns set of new group members
     * 
     * @return set of new group members
     */
    Set<INode> getJoinedMembers();
    
    /**
     * Returns set of left group members.
     * 
     * @return set of left group members
     */
    Set<INode> getLeftMembers();
    
    /**
     * Returns set of failed group members.
     * 
     * @return set of failed group members
     */
    Set<INode> getFailedMembers();
}