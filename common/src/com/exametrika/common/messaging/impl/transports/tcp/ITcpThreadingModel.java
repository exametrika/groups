/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IReceiver;



/**
 * The {@link ITcpThreadingModel} is a threading model of TCP transport.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpThreadingModel
{
    /**
     * Creates receive queue.
     *
     * @param connection connection
     * @param incomingMessageHandler incoming message handler
     * @return receive queue
     */
    ITcpReceiveQueue createReceiveQueue(TcpConnection connection, ITcpIncomingMessageHandler incomingMessageHandler);
    
    /**
     * Creates incoming message handler.
     *
     * @param channelName channel name
     * @param receiver receiver
     * @param serializationRegistry serialization registry
     * @return incoming message handler 
     */
    ITcpIncomingMessageHandler createIncomingMessageHandler(String channelName, IReceiver receiver, 
        ISerializationRegistry serializationRegistry);
}
