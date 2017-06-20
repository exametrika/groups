/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
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
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.WorkerClusterDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackListener;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.exchange.WorkerFeedbackProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.NodeFeedbackProvider;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IWorkerControllerObserver;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerClusterMembershipProtocol;

/**
 * The {@link WorkerToCoreSubChannelFactory} is a worker to core node sub-channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerToCoreSubChannelFactory extends AbstractChannelFactory
{
    private Set<IClusterMembershipListener> clusterMembershipListeners;
    private ClusterMembershipManager clusterMembershipManager;
    private WorkerClusterFailureDetectionProtocol failureDetectionProtocol;
    private List<IWorkerControllerObserver> controllerObservers;
    private NodeFeedbackProvider nodeFeedbackProvider;
    private GroupFeedbackProvider groupFeedbackProvider;
    private DataLossFeedbackProvider dataLossFeedbackProvider;
    
    public WorkerToCoreSubChannelFactory(WorkerNodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public void setClusterMembershipListeners(Set<IClusterMembershipListener> clusterMembershipListeners)
    {
        this.clusterMembershipListeners = clusterMembershipListeners;
    }

    public void setClusterMembershipManager(ClusterMembershipManager clusterMembershipManager)
    {
        this.clusterMembershipManager = clusterMembershipManager;
    }
    
    public void setFailureDetectionProtocol(WorkerClusterFailureDetectionProtocol failureDetectionProtocol)
    {
        this.failureDetectionProtocol = failureDetectionProtocol;
    }
    
    public List<IWorkerControllerObserver> getControllerObservers()
    {
        return controllerObservers;
    }
    
    public NodeFeedbackProvider getNodeFeedbackProvider()
    {
        return nodeFeedbackProvider;
    }

    public GroupFeedbackProvider getGroupFeedbackProvider()
    {
        return groupFeedbackProvider;
    }

    public DataLossFeedbackProvider getDataLossFeedbackProvider()
    {
        return dataLossFeedbackProvider;
    }

    @Override
    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new WorkerNodeTrackingStrategy(failureDetectionProtocol);
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
        
        WorkerClusterDiscoveryProtocol clusterDiscoveryProtocol = new WorkerClusterDiscoveryProtocol(channelName, 
            messageFactory, nodeParameters.discoveryStrategy, liveNodeProvider, clusterMembershipManager,
            nodeFactoryParameters.discoveryPeriod, nodeFactoryParameters.transportChannelTimeout);
        clusterMembershipListeners.add(clusterDiscoveryProtocol);
        protocols.add(clusterDiscoveryProtocol);
        
        List<IClusterMembershipProvider> membershipProviders = new ArrayList<IClusterMembershipProvider>();
        controllerObservers = new ArrayList<IWorkerControllerObserver>();
        WorkerClusterMembershipProtocol clusterMembershipProtocol = new WorkerClusterMembershipProtocol(channelName, 
            messageFactory, clusterMembershipManager, membershipProviders, controllerObservers);
        protocols.add(clusterMembershipProtocol);
        
        NodesMembershipProvider nodesMembershipProvider = new NodesMembershipProvider();
        membershipProviders.add(nodesMembershipProvider);
        
        GroupsMembershipProvider groupsMembershipProvider = new GroupsMembershipProvider();
        membershipProviders.add(groupsMembershipProvider);
        
        List<IFeedbackProvider> feedbackProviders = new ArrayList<IFeedbackProvider>();
        
        groupFeedbackProvider = new GroupFeedbackProvider();
        feedbackProviders.add(groupFeedbackProvider);
       
        nodeFeedbackProvider = new NodeFeedbackProvider();
        feedbackProviders.add(nodeFeedbackProvider);
        
        dataLossFeedbackProvider = new DataLossFeedbackProvider();
        feedbackProviders.add(dataLossFeedbackProvider);
        
        List<IFeedbackListener> feedbackListeners = new ArrayList<IFeedbackListener>();
        WorkerFeedbackProtocol feedbackProtocol = new WorkerFeedbackProtocol(channelName, messageFactory, clusterMembershipManager, 
            feedbackProviders, feedbackListeners, nodeFactoryParameters.dataExchangePeriod);
        protocols.add(feedbackProtocol);
        clusterMembershipListeners.add(feedbackProtocol);
        controllerObservers.add(feedbackProtocol);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
    }
    
    protected void wireSubChannel()
    {
    }
    
    @Override
    protected SubChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new WorkerToCoreSubChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment);
    }
}
