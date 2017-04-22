/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.CompositeChannelFactory;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory.GroupFactoryParameters;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;

/**
 * The {@link CoreNodeChannelFactory} is a core node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreNodeChannelFactory extends CompositeChannelFactory
{
    private ClusterMembershipManager membershipManager;
    
    public CoreNodeChannelFactory(List<AbstractChannelFactory> subChannelFactories, int mainSubChannelIndex)
    {
        super(Arrays.asList(new CoreGroupSubChannelFactory(), new CoreToWorkerSubChannelFactory()), 0);
    }
    
    @Override
    protected ICompositeChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        ICompartment compartment, List<IChannel> subChannels)
    {
        // TODO: parameters
        List<IGracefulExitStrategy> gracefulExitStrategies = ((CoreGroupSubChannelFactory)subChannelFactories.get(0)).getGracefulExitStrategies();
        GroupFactoryParameters groupFactoryParameters = (GroupFactoryParameters)factoryParameters;
        return new CoreNodeChannel(channelName, liveNodeManager, channelObserver, subChannels, 
            subChannels.get(mainSubChannelIndex), compartment, membershipManager, gracefulExitStrategies,
            groupFactoryParameters.gracefulExitTimeout);
    }
}
