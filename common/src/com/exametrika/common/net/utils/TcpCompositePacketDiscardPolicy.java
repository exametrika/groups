/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.util.List;

import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;




/**
 * The {@link TcpCompositePacketDiscardPolicy} is a composite packet discard policy.
 * 
 * @param <T> packet type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpCompositePacketDiscardPolicy<T> implements ITcpPacketDiscardPolicy<T>
{
    private final List<ITcpPacketDiscardPolicy<T>> policies;

    /**
     * Creates a new object.
     *
     * @param policies discard policies
     */
    public TcpCompositePacketDiscardPolicy(List<ITcpPacketDiscardPolicy<T>> policies)
    {
        Assert.notNull(policies);
        
        this.policies = policies;
    }
    
    @Override
    public Object createDigest(T message)
    {
        for (ITcpPacketDiscardPolicy policy : policies)
        {
            Object digest = policy.createDigest(message);
            if (digest != null)
                return digest;
        }
        
        return null;
    }

    @Override
    public void setTimeService(ITimeService timeService)
    {
        for (ITcpPacketDiscardPolicy policy : policies)
            policy.setTimeService(timeService);
    }

    @Override
    public int discardPackets(boolean full, SimpleDeque<TcpPacket> packets)
    {
        int res = 0;
        for (ITcpPacketDiscardPolicy policy : policies)
            res += policy.discardPackets(full, packets);
        
        return res;
    }
}
