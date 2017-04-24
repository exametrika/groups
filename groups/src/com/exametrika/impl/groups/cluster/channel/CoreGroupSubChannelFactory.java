/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.CompositeDeliveryHandler;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.CoreGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.exchange.CoreFeedbackProtocol;
import com.exametrika.impl.groups.cluster.exchange.GroupDataExchangeProtocol;
import com.exametrika.impl.groups.cluster.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackListener;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.failuredetection.CoreCoordinatorClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.NodeFeedbackProvider;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.management.ICommandHandler;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.CoreCoordinatorClusterMembershipProtocol;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.impl.groups.cluster.membership.GroupDefinitionStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.SimpleGroupMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.SimpleWorkerToCoreMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipProvider;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.state.CompositeSimpleStateTransferFactory;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferServerProtocol;
import com.exametrika.spi.groups.ISimpleStateTransferFactory;

/**
 * The {@link CoreGroupSubChannelFactory} is a core group node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreGroupSubChannelFactory extends AbstractChannelFactory
{
    private LocalNodeProvider localNodeProvider;
    private Set<IClusterMembershipListener> clusterMembershipListeners;
    private ClusterMembershipManager clusterMembershipManager;
    private CoreGroupMembershipTracker membershipTracker;
    private CoreGroupMembershipManager membershipManager;
    private List<IGracefulExitStrategy> gracefulExitStrategies = new ArrayList<IGracefulExitStrategy>();
    private List<IFeedbackProvider> feedbackProviders;
    private List<IFeedbackListener> feedbackListeners;
    private IGroupFailureDetector failureDetector;
    private IWorkerNodeDiscoverer workerNodeDiscoverer;
    private NodesMembershipProvider nodesMembershipProvider;
    
    public CoreGroupSubChannelFactory()
    {
        this(new NodeFactoryParameters());
    }
    
    public CoreGroupSubChannelFactory(NodeFactoryParameters factoryParameters)
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

    public List<IGracefulExitStrategy> getGracefulExitStrategies()
    {
        return gracefulExitStrategies;
    }

    public List<IFeedbackProvider> getFeedbackProviders()
    {
        return feedbackProviders;
    }

    public List<IFeedbackListener> getFeedbackListeners()
    {
        return feedbackListeners;
    }

    public CoreGroupMembershipManager getMembershipManager()
    {
        return membershipManager;
    }

    public IGroupFailureDetector getFailureDetector()
    {
        return failureDetector;
    }

    public void setWorkerNodeDiscoverer(IWorkerNodeDiscoverer workerNodeDiscoverer)
    {
        this.workerNodeDiscoverer = workerNodeDiscoverer;
    }

    @Override
    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new GroupNodeTrackingStrategy();
    }
    
    @Override
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        NodeParameters nodeParameters = (NodeParameters)parameters;
        Assert.notNull(nodeParameters.propertyProvider);
        Assert.notNull(nodeParameters.discoveryStrategy);
        Assert.notNull(nodeParameters.stateStore);
        Assert.notNull(nodeParameters.deliveryHandler);
        Assert.notNull(nodeParameters.localFlowController);
        
        NodeFactoryParameters nodeFactoryParameters = (NodeFactoryParameters)factoryParameters;
        
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners = new HashSet<IPreparedGroupMembershipListener>();
        Set<IGroupMembershipListener> membershipListeners = new HashSet<IGroupMembershipListener>();
       
        membershipManager = new CoreGroupMembershipManager(channelName, localNodeProvider, 
            preparedMembershipListeners, membershipListeners);

        Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
        CoreGroupFailureDetectionProtocol failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipManager, 
            failureDetectionListeners, nodeFactoryParameters.failureUpdatePeriod, nodeFactoryParameters.failureHistoryPeriod, 
            nodeFactoryParameters.maxShunCount);
        preparedMembershipListeners.add(failureDetectionProtocol);
        failureObservers.add(failureDetectionProtocol);
        
        CoreGroupDiscoveryProtocol discoveryProtocol = new CoreGroupDiscoveryProtocol(channelName, messageFactory, membershipManager, 
            failureDetectionProtocol, nodeParameters.discoveryStrategy, liveNodeProvider, nodeFactoryParameters.discoveryPeriod, 
            nodeFactoryParameters.groupFormationPeriod);
        preparedMembershipListeners.add(discoveryProtocol);
        membershipListeners.add(discoveryProtocol);
        membershipManager.setNodeDiscoverer(discoveryProtocol);
        
        List<ICommandHandler> commandHandlers = new ArrayList<ICommandHandler>();
        CommandManager commandManager = new CommandManager(channelName, messageFactory, GroupMemberships.CORE_GROUP_ADDRESS, 
            commandHandlers);
        protocols.add(commandManager);
        
        FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, membershipManager);
        protocols.add(flowControlProtocol);
        failureDetectionListeners.add(flowControlProtocol);
        flowControlProtocol.setFailureDetector(failureDetectionProtocol);
        
        List<IClusterMembershipProvider> membershipProviders = new ArrayList<IClusterMembershipProvider>();
        CoreCoordinatorClusterMembershipProtocol clusterMembershipProtocol = new CoreCoordinatorClusterMembershipProtocol(
            channelName, messageFactory, clusterMembershipManager, membershipProviders, membershipManager, 
            nodeFactoryParameters.membershipTrackPeriod);
        protocols.add(clusterMembershipProtocol);
        clusterMembershipProtocol.setFailureDetector(failureDetectionProtocol);
        gracefulExitStrategies.add(clusterMembershipProtocol);
        membershipListeners.add(clusterMembershipProtocol);
        
        GroupDefinitionStateTransferFactory groupDefinitionStateTransferFactory =  new GroupDefinitionStateTransferFactory();
        ISimpleStateTransferFactory stateTransferFactory = new CompositeSimpleStateTransferFactory(Arrays.asList(
            new ClusterMembershipStateTransferFactory(clusterMembershipManager, membershipProviders), 
            groupDefinitionStateTransferFactory));
        
        SimpleStateTransferClientProtocol stateTransferClientProtocol = new SimpleStateTransferClientProtocol(channelName,
            messageFactory, membershipManager, stateTransferFactory, nodeParameters.stateStore);
        protocols.add(stateTransferClientProtocol);
        discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
        failureDetectionListeners.add(stateTransferClientProtocol);
        
        SimpleStateTransferServerProtocol stateTransferServerProtocol = new SimpleStateTransferServerProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, stateTransferFactory, 
            nodeParameters.stateStore, nodeFactoryParameters.saveSnapshotPeriod);
        protocols.add(stateTransferServerProtocol);
        
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, nodeFactoryParameters.maxBundlingMessageSize, 
            nodeFactoryParameters.maxBundlingPeriod, 
            nodeFactoryParameters.maxBundleSize, nodeFactoryParameters.maxTotalOrderBundlingMessageCount, 
            nodeFactoryParameters.maxUnacknowledgedPeriod, nodeFactoryParameters.maxUnacknowledgedMessageCount, 
            nodeFactoryParameters.maxIdleReceiveQueuePeriod, new CompositeDeliveryHandler(
                Arrays.<IDeliveryHandler>asList(commandManager, nodeParameters.deliveryHandler)), true, true, 
            nodeFactoryParameters.maxUnlockQueueCapacity, nodeFactoryParameters.minLockQueueCapacity, 
            serializationRegistry, GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_GROUP_ID);
        protocols.add(multicastProtocol);
        failureDetectionListeners.add(multicastProtocol);
        multicastProtocol.setRemoteFlowController(flowControlProtocol);
        multicastProtocol.setLocalFlowController(nodeParameters.localFlowController);
        flowControlProtocol.setFlowController(multicastProtocol);
        
        List<IFlushParticipant> flushParticipants = new ArrayList<IFlushParticipant>();
        flushParticipants.add(stateTransferClientProtocol);
        flushParticipants.add(stateTransferServerProtocol);
        flushParticipants.add(multicastProtocol);
        FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
           flushParticipants, membershipManager, failureDetectionProtocol);
        protocols.add(flushParticipantProtocol);
        FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
            membershipManager, failureDetectionProtocol, nodeFactoryParameters.flushTimeout, flushParticipantProtocol);
        failureDetectionListeners.add(flushCoordinatorProtocol);
        protocols.add(flushCoordinatorProtocol);
        clusterMembershipProtocol.setFlushManager(flushCoordinatorProtocol);

        GroupDataExchangeProtocol dataExchangeProtocol = new GroupDataExchangeProtocol(channelName, messageFactory, membershipManager,
            failureDetectionProtocol, Arrays.<IDataExchangeProvider>asList(), nodeFactoryParameters.dataExchangePeriod);
        membershipListeners.add(dataExchangeProtocol);
        protocols.add(dataExchangeProtocol);
        failureDetectionListeners.add(dataExchangeProtocol);
        
        feedbackProviders = new ArrayList<IFeedbackProvider>();
        
        GroupFeedbackProvider groupFeedbackProvider = new GroupFeedbackProvider();
        feedbackProviders.add(groupFeedbackProvider);
       
        NodeFeedbackProvider nodeFeedbackProvider = new NodeFeedbackProvider();
        feedbackProviders.add(nodeFeedbackProvider);
        
        DataLossFeedbackProvider dataLossFeedbackProvider = new DataLossFeedbackProvider(nodeParameters.dataLossObserver,
            clusterMembershipManager, groupFeedbackProvider);
        feedbackProviders.add(dataLossFeedbackProvider);
        
        feedbackListeners = new ArrayList<IFeedbackListener>();
        CoreFeedbackProtocol feedbackProtocol = new CoreFeedbackProtocol(channelName, messageFactory, clusterMembershipManager, 
            feedbackProviders, feedbackListeners, nodeFactoryParameters.dataExchangePeriod, failureDetectionProtocol, 
            membershipManager);
        protocols.add(feedbackProtocol);
        clusterMembershipListeners.add(feedbackProtocol);
        failureDetectionListeners.add(feedbackProtocol);
        preparedMembershipListeners.add(feedbackProtocol);
        
        CoreCoordinatorClusterFailureDetectionProtocol clusterFailureDetectionProtocol = new CoreCoordinatorClusterFailureDetectionProtocol(
            channelName, messageFactory, clusterMembershipManager);
        protocols.add(clusterFailureDetectionProtocol);
        
        protocols.add(discoveryProtocol);
        protocols.add(failureDetectionProtocol);
        
        membershipTracker = new CoreGroupMembershipTracker(nodeFactoryParameters.membershipTrackPeriod, membershipManager, discoveryProtocol, 
            failureDetectionProtocol, flushCoordinatorProtocol, nodeFactoryParameters.flushCondition);
        
        gracefulExitStrategies.add(flushCoordinatorProtocol);
        gracefulExitStrategies.add(flushParticipantProtocol);
        gracefulExitStrategies.add(membershipTracker);
        
        nodesMembershipProvider = new NodesMembershipProvider(clusterFailureDetectionProtocol);
        membershipProviders.add(nodesMembershipProvider);
        
        WorkerToCoreMembershipProvider workerToCoreMembershipProvider = new WorkerToCoreMembershipProvider(membershipManager, 
            new SimpleWorkerToCoreMappingStrategy());
        membershipProviders.add(workerToCoreMembershipProvider);
        
        SimpleGroupMappingStrategy groupMappingStrategy = new SimpleGroupMappingStrategy(groupFeedbackProvider, nodeFeedbackProvider);
        commandHandlers.add(groupMappingStrategy);
        GroupsMembershipProvider groupsMembershipProvider = new GroupsMembershipProvider(groupMappingStrategy);
        membershipProviders.add(groupsMembershipProvider);
        groupDefinitionStateTransferFactory.setGroupMappingStrategy(groupMappingStrategy);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
        failureDetectionProtocol.setFailureObserver(transport);
        failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
        channel.getCompartment().addTimerProcessor(membershipTracker);
        
        GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
        strategy.setFailureDetector(failureDetectionProtocol);
        strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        
        FailureAtomicMulticastProtocol multicastProtocol = protocolStack.find(FailureAtomicMulticastProtocol.class);
        multicastProtocol.setCompartment(channel.getCompartment());
        channel.getCompartment().addProcessor(multicastProtocol);
        
        CommandManager commandManager = protocolStack.find(CommandManager.class);
        commandManager.setCompartment(channel.getCompartment());
    }
    
    protected void wireSubChannel()
    {
        nodesMembershipProvider.setNodeDiscoverer(workerNodeDiscoverer);
    }
    
    @Override
    protected SubChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new CoreGroupSubChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment, membershipManager);
    }
}
