/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.SimpleDeque;



/**
 * The {@link ITcpPacketDiscardPolicy} represents a TCP packet discard policy.
 * 
 * @param <T> message type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpPacketDiscardPolicy<T> extends ITcpPacketDigestFactory<T>
{
    /**
     * Sets time service.
     *
     * @param timeService time service
     */
    void setTimeService(ITimeService timeService);

    /**
     * Discards packets from the packet queue.
     *
     * @param full if true packet queue is full
     * @param packets queue of packets to remove packets from. Packets are discarded from the queue by assigning null in 
     * the corresponding packet position in the queue
     * @return total size of discarded packets
     */
    int discardPackets(boolean full, SimpleDeque<TcpPacket> packets);
}
