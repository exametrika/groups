/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CoordinatorCohortNodeTrackingStrategy} is a tracking strategy where coordinator tracks group members and
 * group members track coordinator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class CoordinatorCohortNodeTrackingStrategy implements INodeTrackingStrategy
{
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(liveNodes);
        Assert.isTrue(liveNodes.size() > 1);
        
        if (liveNodes.get(0).equals(localNode))
        {
            Set<IAddress> trackedMembers = new HashSet<IAddress>(liveNodes);
            trackedMembers.remove(localNode);
            
            return trackedMembers;
        }
        
        Assert.isTrue(liveNodes.contains(localNode));
        return Collections.singleton(liveNodes.get(0));
    }
}
