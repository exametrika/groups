/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.CoreNodeFactoryParameters;
import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactoryParameters;
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
    private CoreNodeParameters nodeParameters;
    
    public CoreNodeChannelFactory()
    {
        this(new CoreNodeFactoryParameters());
    }
    
    public CoreNodeChannelFactory(CoreNodeFactoryParameters factoryParameters)
    {
        super(createSubChannelFactories(factoryParameters), 0, factoryParameters);
    }

    public CoreNodeChannelFactory(List<AbstractChannelFactory> subChannelFactories, int mainSubChannelIndex, 
        ChannelFactoryParameters factoryParameters)
    {
        super(subChannelFactories, mainSubChannelIndex, factoryParameters);
    }
    
    public ICompositeChannel createChannel(CoreNodeParameters parameters)
    {
        Assert.notNull(parameters);
        
        nodeParameters = parameters;
        return createChannel(parameters.channelName, Arrays.asList(parameters, parameters.coreToWorkerChannelParameters));
    }
    
    @Override
    protected List<IChannel> createSubChannels(String channelName, List<? extends ChannelParameters> parameters,
        ChannelObserver channelObserver, LiveNodeManager liveNodeManager, TcpNioDispatcher dispatcher,
        ICompartment compartment)
    {
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeManager, nodeParameters.propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        
        Set<IClusterMembershipListener> clusterMembershipListeners = new LinkedHashSet<IClusterMembershipListener>();
        membershipManager = new ClusterMembershipManager(channelName, localNodeProvider, clusterMembershipListeners);
        
        CoreGroupSubChannelFactory coreGroupSubChannelFactory = (CoreGroupSubChannelFactory)subChannelFactories.get(0);
        coreGroupSubChannelFactory.setLocalNodeProvider(localNodeProvider);
        coreGroupSubChannelFactory.setClusterMembershipListeners(clusterMembershipListeners);
        coreGroupSubChannelFactory.setClusterMembershipManager(membershipManager);
        
        CoreToWorkerSubChannelFactory coreToWorkerSubChannelFactory = (CoreToWorkerSubChannelFactory)subChannelFactories.get(1);
        coreToWorkerSubChannelFactory.setClusterMembershipListeners(clusterMembershipListeners);
        coreToWorkerSubChannelFactory.setClusterMembershipManager(membershipManager);
        
        return super.createSubChannels(channelName, parameters, channelObserver, liveNodeManager, dispatcher, compartment);
    }
    
    @Override
    protected void wireSubChannel(int index, List<IChannel> subChannels)
    {
        if (index == 0)
        {
            CoreGroupSubChannelFactory coreGroupSubChannelFactory = (CoreGroupSubChannelFactory)subChannelFactories.get(0);
            CoreToWorkerSubChannelFactory coreToWorkerSubChannelFactory = (CoreToWorkerSubChannelFactory)subChannelFactories.get(1);
            
            coreToWorkerSubChannelFactory.setCoreToWorkerFeedbackProtocol(coreGroupSubChannelFactory.getCoreToWorkerFeedbackProtocol());
        }
    }
    
    @Override
    protected void wireSubChannels(ICompositeChannel channel, List<IChannel> subChannels)
    {
        CoreGroupSubChannelFactory coreGroupSubChannelFactory = (CoreGroupSubChannelFactory)subChannelFactories.get(0);
        CoreToWorkerSubChannelFactory coreToWorkerSubChannelFactory = (CoreToWorkerSubChannelFactory)subChannelFactories.get(1);
        coreToWorkerSubChannelFactory.setFailureDetector(coreGroupSubChannelFactory.getFailureDetector());
        coreToWorkerSubChannelFactory.setBridgeSender(coreGroupSubChannelFactory.getBridgeSender());
        
        coreGroupSubChannelFactory.setWorkerSender(coreToWorkerSubChannelFactory.getWorkerSender());
        coreGroupSubChannelFactory.setChannelReconnector((CoreNodeChannel)channel);
        coreGroupSubChannelFactory.setClusterFailureDetectionProtocol(coreToWorkerSubChannelFactory.getClusterFailureDetectionProtocol());
        coreGroupSubChannelFactory.setCoreToWorkerFailureObserver(coreToWorkerSubChannelFactory.getCoreToWorkerFailureObserver());
        coreGroupSubChannelFactory.setCoreToWorkerFailureDetectionListeners(coreToWorkerSubChannelFactory.getCoreToWorkerFailureDetectionListeners());
        
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
            nodeFactoryParameters.gracefulExitTimeout, nodeParameters.channelReconnector);
    }
    
    private static List<AbstractChannelFactory> createSubChannelFactories(CoreNodeFactoryParameters factoryParameters)
    {
        return Arrays.asList(new CoreGroupSubChannelFactory(factoryParameters), new CoreToWorkerSubChannelFactory(factoryParameters));
    }
}
