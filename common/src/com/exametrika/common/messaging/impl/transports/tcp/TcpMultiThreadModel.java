/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.tasks.ITaskQueue;

/**
 * The {@link TcpMultiThreadModel} represents a multithread TCP threading model.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpMultiThreadModel implements ITcpThreadingModel
{
    private final int receiveThreadCount;

    public TcpMultiThreadModel(int receiveThreadCount)
    {
        this.receiveThreadCount = receiveThreadCount;
    }
    
    @Override
    public ITcpReceiveQueue createReceiveQueue(TcpConnection connection,
        ITcpIncomingMessageHandler incomingMessageHandler)
    {
        return new TcpMtReceiveQueue(connection, (ITaskQueue<TcpMtReceiveTask>)incomingMessageHandler);
    }

    @Override
    public ITcpIncomingMessageHandler createIncomingMessageHandler(String channelName, IReceiver receiver,
        ISerializationRegistry serializationRegistry)
    {
        return new TcpMtIncomingMessageHandler(receiveThreadCount, channelName, receiver, serializationRegistry);
    }
}
