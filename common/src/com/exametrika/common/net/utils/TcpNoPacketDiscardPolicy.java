/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.SimpleDeque;




/**
 * The {@link TcpNoPacketDiscardPolicy} is a packet discard policy that does not discards any packets.
 * 
 * @param <T> packet type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNoPacketDiscardPolicy<T> implements ITcpPacketDiscardPolicy<T>
{
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
        return 0;
    }
}
