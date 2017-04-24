/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.CompositeChannelFactory;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;

/**
 * The {@link CoreNodeChannelFactory} is a core node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreNodeChannelFactory extends CompositeChannelFactory
{
    private ClusterMembershipManager membershipManager;
    
    public CoreNodeChannelFactory()
    {
        this(new CoreNodeFactoryParameters());
    }
    
    public CoreNodeChannelFactory(CoreNodeFactoryParameters factoryParameters)
    {
        super(Arrays.asList(new CoreGroupSubChannelFactory(), new CoreToWorkerSubChannelFactory()), 0, factoryParameters);
    }
    
    public ICompositeChannel createChannel(CoreNodeParameters parameters)
    {
        Assert.notNull(parameters);
        
        return createChannel(parameters.channelName, Arrays.asList(parameters, parameters));
    }
    
    @Override
    protected List<IChannel> createSubChannels(String channelName, List<? extends ChannelParameters> parameters,
        ChannelObserver channelObserver, LiveNodeManager liveNodeManager, TcpNioDispatcher dispatcher,
        ICompartment compartment)
    {
        CoreNodeParameters nodeParameters = (CoreNodeParameters)parameters.get(0);
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeManager, nodeParameters.propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        
        Set<IClusterMembershipListener> clusterMembershipListeners = new LinkedHashSet<IClusterMembershipListener>();
        membershipManager = new ClusterMembershipManager(channelName, localNodeProvider, clusterMembershipListeners);
        
        CoreGroupSubChannelFactory coreGroupSubChannelFactory = (CoreGroupSubChannelFactory)subChannelFactories.get(0);
        coreGroupSubChannelFactory.setLocalNodeProvider(localNodeProvider);
        coreGroupSubChannelFactory.setClusterMembershipListeners(clusterMembershipListeners);
        coreGroupSubChannelFactory.setClusterMembershipManager(membershipManager);
        
        return super.createSubChannels(channelName, parameters, channelObserver, liveNodeManager, dispatcher, compartment);
    }
    
    @Override
    protected void wireSubChannels(List<IChannel> subChannels)
    {
        CoreGroupSubChannelFactory coreGroupSubChannelFactory = (CoreGroupSubChannelFactory)subChannels.get(0);
        CoreToWorkerSubChannelFactory coreToWorkerSubChannelFactory = (CoreToWorkerSubChannelFactory)subChannels.get(1);
        coreGroupSubChannelFactory.setWorkerNodeDiscoverer(coreToWorkerSubChannelFactory.getWorkerNodeDiscoverer());
        coreToWorkerSubChannelFactory.setFailureDetector(coreGroupSubChannelFactory.getFailureDetector());
        coreToWorkerSubChannelFactory.setMembershipService(coreGroupSubChannelFactory.getMembershipManager());
        
        coreGroupSubChannelFactory.wireSubChannel();
        coreToWorkerSubChannelFactory.wireSubChannel();
    }

    @Override
    protected ICompositeChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        ICompartment compartment, List<IChannel> subChannels)
    {
        List<IGracefulExitStrategy> gracefulExitStrategies = ((CoreGroupSubChannelFactory)subChannelFactories.get(0)).getGracefulExitStrategies();
        CoreNodeFactoryParameters nodeFactoryParameters = (CoreNodeFactoryParameters)factoryParameters;
        return new CoreNodeChannel(channelName, liveNodeManager, channelObserver, subChannels, 
            subChannels.get(mainSubChannelIndex), compartment, membershipManager, gracefulExitStrategies,
            nodeFactoryParameters.gracefulExitTimeout);
    }
}
