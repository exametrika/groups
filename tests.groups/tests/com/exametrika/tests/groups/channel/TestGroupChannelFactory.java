/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.CompositeDeliveryHandler;
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
import com.exametrika.impl.groups.cluster.check.GroupCheckStateProtocol;
import com.exametrika.impl.groups.cluster.discovery.CoreGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.exchange.GroupDataExchangeProtocol;
import com.exametrika.impl.groups.cluster.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.state.AsyncStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.AsyncStateTransferServerProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferServerProtocol;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.tests.groups.mocks.DataLossFeedbackProviderMock;

/**
 * The {@link TestGroupChannelFactory} is a test group channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestGroupChannelFactory extends ChannelFactory
{
    private CoreGroupMembershipTracker membershipTracker;
    private CoreGroupMembershipManager membershipManager;
    
    protected CoreGroupFailureDetectionProtocol failureDetectionProtocol;
    protected TestGroupParameters nodeParameters;
    
    private AsyncStateTransferClientProtocol stateTransferClientProtocol;
    private AsyncStateTransferServerProtocol stateTransferServerProtocol;
    
    public TestGroupChannelFactory()
    {
        this(new TestGroupFactoryParameters());
    }
    
    public TestGroupChannelFactory(TestGroupFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public TestGroupChannel createChannel(TestGroupParameters parameters)
    {
        return (TestGroupChannel)super.createChannel(parameters);
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
        nodeParameters = (TestGroupParameters)parameters;
        Assert.notNull(nodeParameters.propertyProvider);
        Assert.notNull(nodeParameters.discoveryStrategy);
        Assert.notNull(nodeParameters.localFlowController);
        
        TestGroupFactoryParameters nodeFactoryParameters = (TestGroupFactoryParameters)factoryParameters;
        
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, nodeParameters.propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners = new HashSet<IPreparedGroupMembershipListener>();
        Set<IGroupMembershipListener> membershipListeners = new HashSet<IGroupMembershipListener>();
       
        membershipManager = new CoreGroupMembershipManager(channelName, localNodeProvider, 
            preparedMembershipListeners, membershipListeners);

        Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
        failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipManager, 
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
        
        GroupCheckStateProtocol checkStateProtocol = new GroupCheckStateProtocol(channelName, messageFactory, membershipManager, 
            new DataLossFeedbackProviderMock(), nodeFactoryParameters.checkStatePeriod, GroupMemberships.CORE_GROUP_ID,
            GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_DOMAIN);
        protocols.add(checkStateProtocol);
        failureDetectionListeners.add(checkStateProtocol);
        checkStateProtocol.setFailureDetector(failureDetectionProtocol);
        
        FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, membershipManager);
        protocols.add(flowControlProtocol);
        failureDetectionListeners.add(flowControlProtocol);
        flowControlProtocol.setFailureDetector(failureDetectionProtocol);
        
        List<IFlushParticipant> flushParticipants = new ArrayList<IFlushParticipant>();
        if (!nodeParameters.asyncStateTransfer)
        {
            SimpleStateTransferClientProtocol stateTransferClientProtocol = new SimpleStateTransferClientProtocol(channelName,
                messageFactory, membershipManager, nodeParameters.stateTransferFactory, GroupMemberships.CORE_GROUP_ID);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            
            SimpleStateTransferServerProtocol stateTransferServerProtocol = new SimpleStateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, nodeParameters.stateTransferFactory, 
                GroupMemberships.CORE_GROUP_ID, nodeFactoryParameters.saveSnapshotPeriod);
            protocols.add(stateTransferServerProtocol);
            flushParticipants.add(stateTransferClientProtocol);
            flushParticipants.add(stateTransferServerProtocol);
            
            checkStateProtocol.setStateHashProvider(stateTransferServerProtocol);
        }
        else
        {
            stateTransferClientProtocol = new AsyncStateTransferClientProtocol(channelName,
                messageFactory, membershipManager, nodeParameters.stateTransferFactory,  GroupMemberships.CORE_GROUP_ID, serializationRegistry,
                nodeFactoryParameters.maxStateTransferPeriod, nodeFactoryParameters.stateSizeThreshold);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            flushParticipants.add(stateTransferClientProtocol);
            
            stateTransferServerProtocol = new AsyncStateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, nodeParameters.stateTransferFactory, serializationRegistry,
                nodeFactoryParameters.saveSnapshotPeriod, nodeFactoryParameters.transferLogRecordPeriod, nodeFactoryParameters.transferLogMessagesCount,
                nodeFactoryParameters.minLockQueueCapacity,  GroupMemberships.CORE_GROUP_ADDRESS,  GroupMemberships.CORE_GROUP_ID);
            protocols.add(stateTransferServerProtocol);
            flushParticipants.add(stateTransferServerProtocol);
            stateTransferServerProtocol.setFlowController(flowControlProtocol);
            
            checkStateProtocol.setStateHashProvider(stateTransferServerProtocol);
        }
        
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, nodeFactoryParameters.maxBundlingMessageSize, 
            nodeFactoryParameters.maxBundlingPeriod, 
            nodeFactoryParameters.maxBundleSize, nodeFactoryParameters.maxTotalOrderBundlingMessageCount, 
            nodeFactoryParameters.maxUnacknowledgedPeriod, nodeFactoryParameters.maxUnacknowledgedMessageCount, 
            nodeFactoryParameters.maxIdleReceiveQueuePeriod, new CompositeDeliveryHandler(
                Arrays.<IDeliveryHandler>asList(nodeParameters.deliveryHandler)), true, true, 
            nodeFactoryParameters.maxUnlockQueueCapacity, nodeFactoryParameters.minLockQueueCapacity, 
            serializationRegistry, GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_GROUP_ID);
        protocols.add(multicastProtocol);
        failureDetectionListeners.add(multicastProtocol);
        multicastProtocol.setRemoteFlowController(flowControlProtocol);
        multicastProtocol.setLocalFlowController(nodeParameters.localFlowController);
        flowControlProtocol.setFlowController(multicastProtocol);
        
        flushParticipants.add(multicastProtocol);
        FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
           flushParticipants, membershipManager, failureDetectionProtocol);
        protocols.add(flushParticipantProtocol);
        FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
            membershipManager, failureDetectionProtocol, nodeFactoryParameters.flushTimeout, flushParticipantProtocol);
        failureDetectionListeners.add(flushCoordinatorProtocol);
        protocols.add(flushCoordinatorProtocol);

        GroupDataExchangeProtocol dataExchangeProtocol = new GroupDataExchangeProtocol(channelName, messageFactory, membershipManager,
            failureDetectionProtocol, Arrays.<IDataExchangeProvider>asList(), nodeFactoryParameters.dataExchangePeriod);
        membershipListeners.add(dataExchangeProtocol);
        protocols.add(dataExchangeProtocol);
        failureDetectionListeners.add(dataExchangeProtocol);
        
        protocols.add(discoveryProtocol);
        protocols.add(failureDetectionProtocol);
        
        membershipTracker = new CoreGroupMembershipTracker(nodeFactoryParameters.membershipTrackPeriod, membershipManager, discoveryProtocol, 
            failureDetectionProtocol, flushCoordinatorProtocol, null);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        failureDetectionProtocol.setFailureObserver(transport);
        channel.getCompartment().addTimerProcessor(membershipTracker);
        
        GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
        strategy.setFailureDetector(failureDetectionProtocol);
        failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
        strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        
        FailureAtomicMulticastProtocol multicastProtocol = protocolStack.find(FailureAtomicMulticastProtocol.class);
        multicastProtocol.setCompartment(channel.getCompartment());
        channel.getCompartment().addProcessor(multicastProtocol);
        
        if (stateTransferClientProtocol != null)
        {
            stateTransferClientProtocol.setCompartment(channel.getCompartment());
            stateTransferClientProtocol.setChannelReconnector((IChannelReconnector)channel);
            
            stateTransferServerProtocol.setCompartment(channel.getCompartment());
        }
    }
    
    @Override
    protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new TestGroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment, membershipManager, nodeParameters.channelReconnector, membershipTracker);
    }
}
