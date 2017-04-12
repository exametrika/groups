/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.Set;
import java.util.UUID;

/**
 * The {@link INodeFeedbackService} represents a node feedback service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface INodeFeedbackService
{
    /**
     * Returns available node states.
     *
     * @return available node states
     */
    Set<INodeState> getNodeStates();
    
    /**
     * Finds node state by node identifier.
     *
     * @param id nide identifier
     * @return node state or null if node state is not found
     */
    INodeState findNodeState(UUID id);
    
    /**
     * Updates node state.
     *
     * @param state node state
     */
    void updateNodeState(INodeState state);
}