/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link RingNodeTrackingStrategy} is a tracking strategy where local node tracks next non-failed node in current 
 * membership. If number of non-failed (healthy) members exceeds {@link RingNodeTrackingStrategy#SINGLE_RING_THRESHOLD}
 * then two level scheme is used with n + 1 rings, where n = sqrt(healthy_members_size). n rings consist of n members each,
 * and one ring of size n consists of ring coordinators (first member of particular ring).
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class RingNodeTrackingStrategy implements INodeTrackingStrategy
{
    public static final int SINGLE_RING_THRESHOLD = 10;
    
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(liveNodes);
        Assert.isTrue(liveNodes.size() > 1);
    
        int pos = liveNodes.indexOf(localNode);
        Assert.isTrue(pos >= 0);
        
        if (liveNodes.size() <= SINGLE_RING_THRESHOLD)
        {
            if (pos < liveNodes.size() - 1)
                return Collections.asSet(liveNodes.get(pos + 1));
            
            return Collections.asSet(liveNodes.get(0));
        }
        
        int ringCount = (int)Math.round(Math.sqrt(liveNodes.size()));
        int minRingSize = liveNodes.size() / ringCount;
        int bigRingSize = minRingSize + 1;
        int bigRingCount = liveNodes.size() % ringCount;

        int smallRingStart = bigRingSize * bigRingCount;
        
        if (pos < smallRingStart)
        {
            int ring1Size = bigRingSize;
            int ring1Number = pos / ring1Size;
            int ring1Start = ring1Number * ring1Size;
            int ring1Offset = pos - ring1Start;
            
            IAddress ring1NextNode;
            if (ring1Offset < ring1Size - 1)
                ring1NextNode = liveNodes.get(ring1Start + ring1Offset + 1);
            else
                ring1NextNode = liveNodes.get(ring1Start);
            
            if (ring1Offset == 0)
            {
                IAddress ring2NextNode;
                if (ring1Number < ringCount - 1)
                    ring2NextNode = liveNodes.get((ring1Number + 1) * ring1Size);
                else
                    ring2NextNode = liveNodes.get(0);
                
                return Collections.asSet(ring1NextNode, ring2NextNode);
            }
            
            return Collections.asSet(ring1NextNode);
            
        }

        int ring1Size = minRingSize;
        int ring1Number = (pos - smallRingStart) / ring1Size;
        int ring1Start = ring1Number * ring1Size + smallRingStart;
        int ring1Offset = pos - ring1Start;
        
        IAddress ring1NextNode;
        if (ring1Offset < ring1Size - 1)
            ring1NextNode = liveNodes.get(ring1Start + ring1Offset + 1);
        else
            ring1NextNode = liveNodes.get(ring1Start);
        
        if (ring1Offset == 0)
        {
            IAddress ring2NextNode;
            if ((bigRingCount + ring1Number) < ringCount - 1)
                ring2NextNode = liveNodes.get(smallRingStart + (ring1Number + 1) * ring1Size);
            else
                ring2NextNode = liveNodes.get(0);
            
            return Collections.asSet(ring1NextNode, ring2NextNode);
        }
        
        return Collections.asSet(ring1NextNode);
    }
}
