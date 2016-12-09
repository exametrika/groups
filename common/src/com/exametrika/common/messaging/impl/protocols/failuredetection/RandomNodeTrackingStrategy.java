/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;

/**
 * The {@link RandomNodeTrackingStrategy} is an tracking strategy where local node tracks some specified
 * number of random group members.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class RandomNodeTrackingStrategy implements INodeTrackingStrategy
{
    private final int trackCount;
    private final Random random = new Random();

    /**
     * Creates a new object.
     *
     * @param trackCount number of tracked nodes
     */
    public RandomNodeTrackingStrategy(int trackCount)
    {
        this.trackCount = trackCount;
    }
    
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(liveNodes);
        Assert.isTrue(liveNodes.size() > 1);
        
        liveNodes = new ArrayList<IAddress>(liveNodes);
        Assert.isTrue(liveNodes.remove(localNode));
        
        if (trackCount >= liveNodes.size())
            return new HashSet<IAddress>(liveNodes);
        
        Set<IAddress> trackedMembers = new HashSet<IAddress>();
        for (int i = 0; i < trackCount; i++)
        {
            int k = random.nextInt(liveNodes.size());
            trackedMembers.add(liveNodes.remove(k));
        }
        
        return trackedMembers;
    }
}
