/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.CompositeChannelFactory;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupLeaveGracefulExitStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;

/**
 * The {@link WorkerNodeChannelFactory} is a worker node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerNodeChannelFactory extends CompositeChannelFactory
{
    private ClusterMembershipManager membershipManager;
    private WorkerNodeParameters nodeParameters;
    
    public WorkerNodeChannelFactory()
    {
        this(new WorkerNodeFactoryParameters());
    }
    
    public WorkerNodeChannelFactory(WorkerNodeFactoryParameters factoryParameters)
    {
        super(Arrays.asList(new WorkerGroupSubChannelFactory(), new WorkerToCoreSubChannelFactory()), 0, factoryParameters);
    }
    
    public ICompositeChannel createChannel(WorkerNodeParameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.stateTransferFactory);
        Assert.notNull(parameters.deliveryHandler);
        
        nodeParameters = parameters;
        return createChannel(parameters.channelName, Arrays.asList(parameters, parameters));
    }
    
    @Override
    protected List<IChannel> createSubChannels(String channelName, List<? extends ChannelParameters> parameters,
        ChannelObserver channelObserver, LiveNodeManager liveNodeManager, TcpNioDispatcher dispatcher,
        ICompartment compartment)
    {
        WorkerNodeParameters nodeParameters = (WorkerNodeParameters)parameters.get(0);
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeManager, nodeParameters.propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        
        Set<IClusterMembershipListener> clusterMembershipListeners = new LinkedHashSet<IClusterMembershipListener>();
        membershipManager = new ClusterMembershipManager(channelName, localNodeProvider, clusterMembershipListeners);
        
        WorkerGroupSubChannelFactory workerGroupSubChannelFactory = (WorkerGroupSubChannelFactory)subChannelFactories.get(0);
        workerGroupSubChannelFactory.setLocalNodeProvider(localNodeProvider);
        workerGroupSubChannelFactory.setClusterMembershipListeners(clusterMembershipListeners);
        workerGroupSubChannelFactory.setClusterMembershipManager(membershipManager);
        
        WorkerToCoreSubChannelFactory workerToCoreSubChannelFactory = (WorkerToCoreSubChannelFactory)subChannelFactories.get(1);
        workerToCoreSubChannelFactory.setClusterMembershipListeners(clusterMembershipListeners);
        workerToCoreSubChannelFactory.setClusterMembershipManager(membershipManager);
        
        return super.createSubChannels(channelName, parameters, channelObserver, liveNodeManager, dispatcher, compartment);
    }
    
    @Override
    protected void wireSubChannel(int index, List<IChannel> subChannels)
    {
        if (index == 0)
        {
            WorkerGroupSubChannelFactory workerGroupSubChannelFactory = (WorkerGroupSubChannelFactory)subChannelFactories.get(0);
            WorkerToCoreSubChannelFactory workerToCoreSubChannelFactory = (WorkerToCoreSubChannelFactory)subChannelFactories.get(1);
            
            workerToCoreSubChannelFactory.setFailureDetectionProtocol(workerGroupSubChannelFactory.getFailureDetectionProtocol());        }
    }
    
    @Override
    protected void wireSubChannels(ICompositeChannel channel, List<IChannel> subChannels)
    {
        WorkerGroupSubChannelFactory workerGroupSubChannelFactory = (WorkerGroupSubChannelFactory)subChannels.get(0);
        workerGroupSubChannelFactory.setChannelReconnector((WorkerNodeChannel)channel);
        
        WorkerToCoreSubChannelFactory workerToCoreSubChannelFactory = (WorkerToCoreSubChannelFactory)subChannelFactories.get(1);
        workerGroupSubChannelFactory.setControllerObservers(workerToCoreSubChannelFactory.getControllerObservers());
        workerGroupSubChannelFactory.setDataLossFeedbackProvider(workerToCoreSubChannelFactory.getDataLossFeedbackProvider());
        workerGroupSubChannelFactory.setGroupFeedbackProvider(workerToCoreSubChannelFactory.getGroupFeedbackProvider());
        
        workerGroupSubChannelFactory.wireSubChannel();
        workerToCoreSubChannelFactory.wireSubChannel();
    }
    
    @Override
    protected ICompositeChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        ICompartment compartment, List<IChannel> subChannels)
    {
        WorkerNodeFactoryParameters nodeFactoryParameters = (WorkerNodeFactoryParameters)factoryParameters;
        List<IGracefulExitStrategy> gracefulExitStrategies = new ArrayList<IGracefulExitStrategy>();
        gracefulExitStrategies.add(new GroupLeaveGracefulExitStrategy(membershipManager));
       
        WorkerToCoreSubChannelFactory workerToCoreSubChannelFactory = (WorkerToCoreSubChannelFactory)subChannelFactories.get(1);
        INodeFeedbackService feedbackService = workerToCoreSubChannelFactory.getNodeFeedbackProvider();
       
        return new WorkerNodeChannel(channelName, liveNodeManager, channelObserver, subChannels, 
            subChannels.get(mainSubChannelIndex), compartment, membershipManager, gracefulExitStrategies,
            nodeFactoryParameters.gracefulExitTimeout, feedbackService, nodeParameters.channelReconnector);
    }
}
