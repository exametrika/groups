/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.List;


/**
 * The {@link IFlushParticipantWithCoordinatorState} is a flush participant with coordinator state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlushParticipantWithCoordinatorState extends IFlushParticipant
{
    /**
     * Is coordinator state supported by this participant? Coordinator state is centralized group state kept on coordinator.
     * If coordinator is failed, coordinator state is restored by requesting its parts from other group nodes and
     * combining resulting state on new coordinator.
     *
     * @return true if coordinator state is supported
     */
    boolean isCoordinatorStateSupported();
    
    /**
     * Called on each group node when group coordinator has been failed. Returns state sufficient for restoring 
     * new coordinator state.
     *
     * @return part of coordinator state, stored on local node and used for restoring new coordinator state
     */
    Object getCoordinatorState();
    
    /**
     * Called on new coordinator after coordinator state has been requested from each group node.
     *
     * @param states list of coordinator states kept on each group node, sufficient for restoring new coordinator state
     */
    void setCoordinatorState(List<Object> states);
}
