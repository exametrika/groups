/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.ICoreNodeChannel;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;

/**
 * The {@link CoreNodeChannel} is a core node channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreNodeChannel extends NodeChannel implements ICoreNodeChannel
{
    public CoreNodeChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver,
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory,
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider,
            compartment, membershipManager, gracefulExitStrategies, gracefulExitTimeout);
    }
}
