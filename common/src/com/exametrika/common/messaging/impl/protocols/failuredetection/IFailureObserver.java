/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Set;

import com.exametrika.common.messaging.IAddress;



/**
 * The {@link IFailureObserver} is a node failure observer.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFailureObserver
{
    /**
     * Notifies failure observer that specified nodes are failed.
     *
     * @param nodes set of failed nodes
     */
    void onNodesFailed(Set<IAddress> nodes);
    
    /**
     * Notifies failure observer that specified nodes are left.
     *
     * @param nodes set of left nodes
     */
    void onNodesLeft(Set<IAddress> nodes);
}
