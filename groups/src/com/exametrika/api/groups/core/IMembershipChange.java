/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;

import java.util.Set;


/**
 * The {@link IMembershipChange} represents a difference between old and new membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IMembershipChange
{
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