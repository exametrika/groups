/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.List;
import java.util.UUID;


/**
 * The {@link IGroupState} represents a group state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupState
{
    /** Group state.*/
    enum State
    {
        /** Normal group state.*/
        NORMAL,
        /** Flush in progress group state.*/
        FLUSH
    }
    
    /**
     * Returns group domain.
     *
     * @return group domain
     */
    String getDomain();
    
    /**
     * Returns group identifier.
     *
     * @return group identifier
     */
    UUID getId();
    
    /**
     * Returns identifier of group membership.
     *
     * @return identifier of group membership
     */
    long getMembershipId();
    
    /**
     * Returns list of group members.
     *
     * @return list of group members
     */
    List<UUID> getMembers();
    
    /**
     * Does group belong to primary partition?
     *
     * @return true if group belongs to primary partition
     */
    boolean isPrimary();
    
    /**
     * Returns group state.
     *
     * @return group state
     */
    State getState();
}