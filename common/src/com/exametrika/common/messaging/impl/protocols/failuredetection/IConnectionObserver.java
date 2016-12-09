/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Set;

import com.exametrika.common.messaging.IAddress;



/**
 * The {@link IConnectionObserver} is a node connection observer.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConnectionObserver
{
    /**
     * Notifies observer that specified nodes are connected and must be registered as live nodes.
     *
     * @param nodes set of new connected live nodes
     */
    void onNodesConnected(Set<IAddress> nodes);
}
