/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ClusterMembershipMessagePart} is a cluster membership message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipMessagePart implements IMessagePart
{
    private final long roundId;
    private final ClusterMembershipDelta delta;

    public ClusterMembershipMessagePart(long roundId, ClusterMembershipDelta delta)
    {
        Assert.notNull(delta);
        
        this.roundId = roundId;
        this.delta = delta;
    }
    
    public long getRoundId()
    {
        return roundId;
    }
    
    public ClusterMembershipDelta getDelta()
    {
        return delta;
    }
    
    @Override
    public int getSize()
    {
        return 65000;
    }
    
    @Override 
    public String toString()
    {
        return roundId + ":" + delta.toString();
    }
}

