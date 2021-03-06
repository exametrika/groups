/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;

/**
 * The {@link WorkerGroupSubChannel} is a worker group node sub-channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerGroupSubChannel extends SubChannel
{
    public WorkerGroupSubChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory, 
        IConnectionProvider connectionProvider, ICompartment compartment)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider, compartment);
    }
}
