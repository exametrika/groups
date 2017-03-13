/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link ClusterMembershipResponseMessagePart} is a cluster membership response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipResponseMessagePart implements IMessagePart
{
    private final long roundId;

    public ClusterMembershipResponseMessagePart(long roundId)
    {
        this.roundId = roundId;
    }
    
    public long getRoundId()
    {
        return roundId;
    }
    
    @Override
    public int getSize()
    {
        return 8;
    }
    
    @Override 
    public String toString()
    {
        return Long.toString(roundId);
    }
}

