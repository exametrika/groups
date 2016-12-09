/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link FullNodeTrackingStrategy} is a tracking strategy where local node tracks all live nodes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class FullNodeTrackingStrategy implements INodeTrackingStrategy
{
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        Set<IAddress> trackedNodes = new HashSet(liveNodes);
        trackedNodes.remove(localNode);
        return trackedNodes;
    }
}
