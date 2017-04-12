/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.UUID;


/**
 * The {@link INodeState} represents a node state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface INodeState
{
    /** Node state.*/
    enum State
    {
        /** Normal node state.*/
        NORMAL,
        /** Graceful exit has been requested.*/
        GRACEFUL_EXIT_REQUESTED
    }
    
    /**
     * Returns node domain.
     *
     * @return node domain
     */
    String getDomain();
    
    /**
     * Returns node identifier.
     *
     * @return node identifier
     */
    UUID getId();
    
    /**
     * Returns node state.
     *
     * @return node state
     */
    State getState();
}