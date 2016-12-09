/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpStReceiveQueue} represents a single-threaded receive queue of tcp connection.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpStReceiveQueue implements ITcpReceiveQueue
{
    private final TcpConnection connection;
    private final IReceiver receiver;
    private final ISerializationRegistry serializationRegistry;
    private int lockCount;
    
    public TcpStReceiveQueue(TcpConnection connection, TcpStIncomingMessageHandler incomingMessageHandler)
    {
        Assert.notNull(connection);
        Assert.notNull(incomingMessageHandler);
        
        this.connection = connection;
        this.receiver = incomingMessageHandler.getReceiver();
        this.serializationRegistry = incomingMessageHandler.getSerializationRegistry();
    }
    
    @Override
    public void lockFlow()
    {
        lockCount++;
        
        if (lockCount == 1)
            connection.updateReadStatus();
    }
    
    @Override
    public void unlockFlow()
    {
        lockCount--;
        
        if (lockCount == 0)
            connection.updateReadStatus();
    }
    
    @Override
    public boolean canRead(ITcpChannel channel)
    {
        return lockCount == 0;
    }

    @Override
    public void onRead(ITcpChannel channel)
    {
        TcpPacket packet = ((ITcpPacketChannel<TcpPacket>)channel).read();
        if (packet == null)
            return;
        
        IMessage message = MessageSerializers.deserialize(serializationRegistry, connection.getRemoteAddress(), 
            connection.getLocalAddress(), packet, TcpTransport.HEADER_OVERHEAD);
        
        receiver.receive(message);
    }
}