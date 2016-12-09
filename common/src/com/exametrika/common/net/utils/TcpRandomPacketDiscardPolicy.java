/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.util.Random;

import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.SimpleDeque;




/**
 * The {@link TcpRandomPacketDiscardPolicy} is a packet discard policy that discards random packet on queue overflow.
 * 
 * @param <T> packet type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpRandomPacketDiscardPolicy<T> implements ITcpPacketDiscardPolicy<T>
{
    private final Random random = new Random();

    @Override
    public Object createDigest(T message)
    {
        return null;
    }

    @Override
    public void setTimeService(ITimeService timeService)
    {
    }

    @Override
    public int discardPackets(boolean full, SimpleDeque<TcpPacket> packets)
    {
        if (!full)
            return 0;
        
        int index = random.nextInt(packets.size());
        
        TcpPacket packet = packets.get(index);
        packets.set(index, null);
        
        if (packet != null)
        {
            packet.cleanup();
            return packet.getSize();
        }
        else
            return 0;
    }
}
