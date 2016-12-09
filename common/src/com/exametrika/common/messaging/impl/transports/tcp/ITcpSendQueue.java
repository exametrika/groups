/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelWriter;


/**
 * The {@link ITcpSendQueue} is a TCP send queue.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpSendQueue extends ITcpChannelWriter
{
    /**
     * Sets connection.
     *
     * @param connection connection
     */
    void setConnection(TcpConnection connection);
    
    /**
     * Sets channel.
     *
     * @param channel channel
     */
    void setChannel(ITcpChannel channel);
    
    /**
     * Does queue have capacity to add new packets.
     *
     * @return true if queue has capacity to add new packets
     */
    boolean hasCapacity();
    
    /**
     * Adds new message to the queue. May be blocked on multi-threaded version of queue if queue does not have
     * enough capacity to add new message.
     *
     * @param message message to add
     */
    void put(IMessage message);
    
    /**
     * Adds new message to the queue.
     *
     * @param message message to add
     * @return true if queue has capacity to add new messages false if queue does not have capacity to add new messages
     */
    boolean offer(IMessage message);
    
    /**
     * Closes queue.
     */
    void close();
}
