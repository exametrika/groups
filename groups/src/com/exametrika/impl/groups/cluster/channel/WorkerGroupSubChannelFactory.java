/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.failuredetection.NoneNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackProvider;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IWorkerControllerObserver;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerGroupMembershipProtocol;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link WorkerGroupSubChannelFactory} is a worker group node sub-channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerGroupSubChannelFactory extends AbstractChannelFactory
{
    private LocalNodeProvider localNodeProvider;
    private Set<IClusterMembershipListener> clusterMembershipListeners;
    private ClusterMembershipManager clusterMembershipManager;
    private IChannelReconnector channelReconnector;
    private WorkerClusterFailureDetectionProtocol failureDetectionProtocol;
    private List<IWorkerControllerObserver> controllerObservers;
    private GroupProtocolSubStackFactory protocolSubStackFactory;
    private GroupFeedbackProvider groupFeedbackProvider;
    private DataLossFeedbackProvider dataLossFeedbackProvider;
    
    public WorkerGroupSubChannelFactory()
    {
        this(new WorkerNodeFactoryParameters());
    }
    
    public WorkerGroupSubChannelFactory(WorkerNodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public void setLocalNodeProvider(LocalNodeProvider localNodeProvider)
    {
        this.localNodeProvider = localNodeProvider;
    }

    public void setClusterMembershipListeners(Set<IClusterMembershipListener> clusterMembershipListeners)
    {
        this.clusterMembershipListeners = clusterMembershipListeners;
    }

    public void setClusterMembershipManager(ClusterMembershipManager clusterMembershipManager)
    {
        this.clusterMembershipManager = clusterMembershipManager;
    }
    
    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        this.channelReconnector = channelReconnector;
    }
    
    public void setControllerObservers(List<IWorkerControllerObserver> controllerObservers)
    {
        this.controllerObservers = controllerObservers;
    }

    public void setGroupFeedbackProvider(GroupFeedbackProvider groupFeedbackProvider)
    {
        this.groupFeedbackProvider = groupFeedbackProvider;
    }

    public void setDataLossFeedbackProvider(DataLossFeedbackProvider dataLossFeedbackProvider)
    {
        this.dataLossFeedbackProvider = dataLossFeedbackProvider;
    }

    public WorkerClusterFailureDetectionProtocol getFailureDetectionProtocol()
    {
        return failureDetectionProtocol;
    }
    
    @Override
    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new NoneNodeTrackingStrategy();
    }
    
    @Override
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        WorkerNodeParameters nodeParameters = (WorkerNodeParameters)parameters;
        Assert.notNull(nodeParameters.propertyProvider);
        Assert.notNull(nodeParameters.discoveryStrategy);
        Assert.notNull(nodeParameters.deliveryHandler);
        Assert.notNull(nodeParameters.localFlowController);
        
        WorkerNodeFactoryParameters nodeFactoryParameters = (WorkerNodeFactoryParameters)factoryParameters;
        
        failureDetectionProtocol = new WorkerClusterFailureDetectionProtocol(
            channelName, messageFactory, clusterMembershipManager, nodeFactoryParameters.failureHistoryPeriod, 
            nodeFactoryParameters.maxShunCount, nodeFactoryParameters.nodeOrphanPeriod);
        protocols.add(failureDetectionProtocol);
        clusterMembershipListeners.add(failureDetectionProtocol);
        failureObservers.add(failureDetectionProtocol);
        
        protocolSubStackFactory = new GroupProtocolSubStackFactory(channelName, messageFactory, localNodeProvider, 
            clusterMembershipManager, serializationRegistry,
            nodeFactoryParameters, nodeParameters);
        WorkerGroupMembershipProtocol workerGroupMembershipProtocol = new WorkerGroupMembershipProtocol(channelName, 
            messageFactory, clusterMembershipManager, protocolSubStackFactory, nodeFactoryParameters.groupSubStackRemoveDelay, 
            nodeFactoryParameters.maxSubStackPendingMessageCount);
        protocols.add(workerGroupMembershipProtocol);
        clusterMembershipListeners.add(workerGroupMembershipProtocol);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        protocolSubStackFactory.setCompartment(channel.getCompartment());
        protocolSubStackFactory.setFailureObserver(transport);
    }
    
    protected void wireSubChannel()
    {
        failureDetectionProtocol.setChannelReconnector(channelReconnector);
        controllerObservers.add(failureDetectionProtocol);
        protocolSubStackFactory.setChannelReconnector(channelReconnector);
        protocolSubStackFactory.setDataLossFeedbackProvider(dataLossFeedbackProvider);
        protocolSubStackFactory.setGroupFeedbackProvider(groupFeedbackProvider);
    }
    
    @Override
    protected SubChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new WorkerGroupSubChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment);
    }
}
