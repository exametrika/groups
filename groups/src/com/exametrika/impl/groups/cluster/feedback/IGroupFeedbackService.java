/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.Set;
import java.util.UUID;

/**
 * The {@link IGroupFeedbackService} represents a group feedback service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupFeedbackService
{
    /**
     * Returns available group states.
     *
     * @return available group states
     */
    Set<IGroupState> getGroupStates();
    
    /**
     * Finds group state by group identifier.
     *
     * @param id group identifier
     * @return group state or null if group state is not found
     */
    IGroupState findGroupState(UUID id);
    
    /**
     * Updates group state.
     *
     * @param state state
     */
    void updateGroupState(IGroupState state);
}