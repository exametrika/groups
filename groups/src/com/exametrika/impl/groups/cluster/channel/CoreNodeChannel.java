/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.ICoreNodeChannel;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link CoreNodeChannel} is a core node channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreNodeChannel extends NodeChannel implements ICoreNodeChannel
{
    public CoreNodeChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        List<IChannel> subChannels, IChannel mainSubChannel, ICompartment compartment, ClusterMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout, IChannelReconnector channelReconnector)
    {
        super(channelName, liveNodeManager, channelObserver, subChannels, mainSubChannel, compartment, membershipManager, 
            gracefulExitStrategies, gracefulExitTimeout, channelReconnector);
    }
}
