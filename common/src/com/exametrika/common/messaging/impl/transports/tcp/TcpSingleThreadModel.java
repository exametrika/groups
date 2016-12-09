/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IReceiver;

/**
 * The {@link TcpSingleThreadModel} represents a single thread TCP threading model.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpSingleThreadModel implements ITcpThreadingModel
{
    @Override
    public ITcpReceiveQueue createReceiveQueue(TcpConnection connection,
        ITcpIncomingMessageHandler incomingMessageHandler)
    {
        return new TcpStReceiveQueue(connection, (TcpStIncomingMessageHandler)incomingMessageHandler);
    }
    
    @Override
    public ITcpIncomingMessageHandler createIncomingMessageHandler(String channelName, IReceiver receiver,
        ISerializationRegistry serializationRegistry)
    {
        return new TcpStIncomingMessageHandler(receiver, serializationRegistry);
    }
}
