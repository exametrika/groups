/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.net.utils.ITcpPacketDiscardPolicy;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.SimpleDeque;

/**
 * The {@link TcpStSendQueue} represents a single-threaded send packet queue of tcp connection.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class TcpStSendQueue implements ITcpSendQueue
{
    private TcpConnection connection;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private final IFlowController<IAddress> flowController;
    private final ITcpPacketDiscardPolicy<IMessage> discardPolicy;
    private final ISerializationRegistry serializationRegistry;
    private final SimpleDeque<TcpPacket> highQueue = new SimpleDeque<TcpPacket>();
    private final SimpleDeque<TcpPacket> queue = new SimpleDeque<TcpPacket>();
    private final SimpleDeque<TcpPacket> lowQueue = new SimpleDeque<TcpPacket>();
    private volatile int queueCapacity;
    private boolean flowLocked;
    private boolean closed;
    
    public TcpStSendQueue(TcpConnection connection, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IFlowController<IAddress> flowController, ITcpPacketDiscardPolicy<IMessage> discardPolicy)
    {
        Assert.notNull(connection);
        Assert.notNull(flowController);
        Assert.notNull(discardPolicy);
        
        this.connection = connection;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
        this.flowController = flowController;
        this.discardPolicy = discardPolicy;
        this.serializationRegistry = connection.getSerializationRegistry();
    }
    
    @Override
    public void setConnection(TcpConnection connection)
    {
        Assert.notNull(connection);
        Assert.notNull(connection.getChannel());
        
        this.connection = connection;
    }
    
    @Override
    public void setChannel(ITcpChannel channel)
    {
        Assert.notNull(channel);
    }
    
    @Override
    public boolean hasCapacity()
    {
        return queueCapacity <= maxUnlockQueueCapacity;
    }
    
    @Override
    public void put(IMessage message)
    {
        offer(message);
    }
    
    @Override
    public boolean offer(IMessage message)
    {
        if (closed)
            return false;
        
        Object digest = discardPolicy.createDigest(message);
        TcpPacket packet = MessageSerializers.serialize(serializationRegistry, (Message)message, 
            TcpTransport.HEADER_OVERHEAD, digest);
        byte flags = getFlags(message);
        
        ByteArray buffer = packet.getBuffers().get(0);
        buffer.getBuffer()[buffer.getOffset()] = flags;
        if (!flowLocked && queueCapacity + packet.getSize() >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(connection.getRemoteAddress());
        }
        
        SimpleDeque<TcpPacket> packetQueue;
        if ((flags & TcpTransport.FLAG_HIGH_PRIORITY) == TcpTransport.FLAG_HIGH_PRIORITY)
            packetQueue = highQueue;
        else if ((flags & TcpTransport.FLAG_LOW_PRIORITY) == TcpTransport.FLAG_LOW_PRIORITY)
            packetQueue = lowQueue;
        else
            packetQueue = queue;
        
        packetQueue.offer(packet);
        queueCapacity += packet.getSize();
        
        queueCapacity -= discardPolicy.discardPackets(flowLocked, packetQueue);
        
        connection.updateWriteStatus();
        
        return !flowLocked;
    }
    
    @Override
    public void close()
    {
        closed = true;
        
        if (flowLocked)
        {
            flowController.unlockFlow(connection.getRemoteAddress());
            flowLocked = false;
        }
        
        highQueue.clear();
        queue.clear();
        lowQueue.clear();
    }
    
    @Override
    public boolean canWrite(ITcpChannel channel)
    {
        if (queueCapacity > 0)
            return true;
        
        for (TcpSink sink : connection.getSinks())
            if (sink.canWrite(channel))
                return true;
        
        return false;
    }

    @Override
    public void onWrite(ITcpChannel channel)
    {
        TcpPacket packet = highQueue.peekIgnoreNulls();
        if (packet != null)
        {
            write(channel, packet, highQueue);
            return;
        }
        
        packet = queue.peekIgnoreNulls();
        if (packet != null)
        {
            write(channel, packet, queue);
            return;
        }
        
        packet = lowQueue.peekIgnoreNulls();
        if (packet != null)
        {
            write(channel, packet, lowQueue);
            return;
        }
        
        Assert.checkState(queueCapacity == 0);
        
        boolean written = false;
        for (TcpSink sink : connection.getSinks())
        {
            if (sink.onWrite(channel))
                written = true;
        }
        
        if (!written)
            channel.updateWriteStatus();
    }
    
    private void write(ITcpChannel channel, TcpPacket packet, SimpleDeque<TcpPacket> queue)
    {
        if (((ITcpPacketChannel<TcpPacket>)channel).write(packet))
        {
            queue.poll();
            queueCapacity -= packet.getSize();
            if (flowLocked && queueCapacity <= maxUnlockQueueCapacity)
            {
                flowLocked = false;
                flowController.unlockFlow(connection.getRemoteAddress());
            }
        }
    }
    
    private static byte getFlags(IMessage message)
    {
        byte flags = 0;
        if (message.hasFlags(MessageFlags.PARALLEL))
            flags |= TcpTransport.FLAG_PARALLEL;
        if (message.hasFlags(MessageFlags.HIGH_PRIORITY))
            flags |= TcpTransport.FLAG_HIGH_PRIORITY;
        if (message.hasFlags(MessageFlags.LOW_PRIORITY))
            flags |= TcpTransport.FLAG_LOW_PRIORITY;
        
        return flags;
    }
}