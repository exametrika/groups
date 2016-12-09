/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link TcpMtReceiveQueue} represents a multithreaded receive queue of tcp connection.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpMtReceiveQueue extends TcpMtReceiveTask implements ITcpReceiveQueue
{
    private final ITaskQueue<TcpMtReceiveTask> messageQueue;
    
    public TcpMtReceiveQueue(TcpConnection connection, ITaskQueue<TcpMtReceiveTask> messageQueue)
    {
        super(connection, null);
        
        Assert.notNull(messageQueue);
        
        this.messageQueue = messageQueue;
    }
    
    @Override
    public void onCompleted()
    {
        packet = null;
        connection.updateReadStatus();
    }

    @Override
    public boolean canRead(ITcpChannel channel)
    {
        return packet == null;
    }

    @Override
    public void onRead(ITcpChannel channel)
    {
        if (packet != null)
            return;
        
        TcpPacket packet = ((ITcpPacketChannel<TcpPacket>)channel).read();
        if (packet == null)
            return;
        
        Assert.isTrue(packet.getBuffers().size() == 1);
        ByteArray buffer = packet.getBuffers().get(0);
        byte flags = buffer.getBuffer()[buffer.getOffset()];
        
        if ((flags & TcpTransport.FLAG_PARALLEL) == TcpTransport.FLAG_PARALLEL)
            messageQueue.offer(new TcpMtReceiveTask(connection, packet));
        else
        {
            this.packet = packet;
            channel.updateReadStatus();
            
            messageQueue.offer(this);
        }
    }

    @Override
    public void lockFlow()
    {
    }

    @Override
    public void unlockFlow()
    {
    }
}