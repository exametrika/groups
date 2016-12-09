/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;


/**
 * The {@link INodeTrackingStrategy} is a strategy for determining nodes that must be tracked by failure detector.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface INodeTrackingStrategy
{
    /**
     * Returns set of nodes which must be tracked by failure detector.
     *
     * @param localNode local node
     * @param liveNodes current live (non-failed) nodes (local node is included)
     * @return tracked by failure detector nodes
     */
    Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes);
}
