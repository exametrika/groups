/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroupChannel;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.NoDeliveryHandler;
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
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.CoreGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.exchange.GroupDataExchangeProtocol;
import com.exametrika.impl.groups.cluster.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushCondition;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.impl.groups.cluster.state.StateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.StateTransferServerProtocol;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.spi.groups.IPropertyProvider;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.SystemPropertyProvider;

/**
 * The {@link GroupChannelFactory} is a group channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupChannelFactory extends ChannelFactory
{
    private CoreGroupMembershipTracker membershipTracker;
    private CoreGroupMembershipManager membershipManager;
    private List<IGracefulExitStrategy> gracefulExitStrategies = new ArrayList<IGracefulExitStrategy>();
    
    public static class GroupFactoryParameters extends FactoryParameters
    {
        public long discoveryPeriod = 500;
        public long groupFormationPeriod = 10000;
        public long failureUpdatePeriod = 500;
        public long failureHistoryPeriod = 600000;
        public int maxShunCount = 3;
        public long flushTimeout = 300000;
        public long membershipTrackPeriod = 1000;
        public long gracefulExitTimeout = 10000;
        public long maxStateTransferPeriod = Integer.MAX_VALUE;
        public long stateSizeThreshold = 100000;// TODO: прописать остальные значения по умолчанию
        public long saveSnapshotPeriod = 1000;
        public long transferLogRecordPeriod = 1000;
        public int transferLogMessagesCount = 2;
        public int minLockQueueCapacity = 10000000;
        public int maxUnlockQueueCapacity = 100000;
        public long dataExchangePeriod = 200;
        public int maxBundlingMessageSize = 10000;
        public long maxBundlingPeriod = 100;
        public int maxBundleSize = 10000;
        public int maxTotalOrderBundlingMessageCount = 10;
        public long maxUnacknowledgedPeriod = 100;
        public int maxUnacknowledgedMessageCount = 100;
        public long maxIdleReceiveQueuePeriod = 600000;
        public IFlushCondition flushCondition;
        
        public GroupFactoryParameters()
        {
            super(false);
        }
        
        public GroupFactoryParameters(boolean debug)
        {
            super(debug);
            
            int timeMultiplier = !debug ? 1 : 1000;
            flushTimeout *= timeMultiplier;
            gracefulExitTimeout *= timeMultiplier;
        }
    }
    
    public static class GroupParameters extends Parameters
    {
        public IPropertyProvider propertyProvider = new SystemPropertyProvider();
        public IDiscoveryStrategy discoveryStrategy;
        public IStateStore stateStore;
        public IStateTransferFactory stateTransferFactory;
        public IDeliveryHandler deliveryHandler = new NoDeliveryHandler();
        public IFlowController<RemoteFlowId> localFlowController = new NoFlowController<RemoteFlowId>();
    }

    public GroupChannelFactory()
    {
        this(new GroupFactoryParameters());
    }
    
    public GroupChannelFactory(GroupFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public IGroupChannel createChannel(GroupParameters parameters)
    {
        return (IGroupChannel)super.createChannel(parameters);
    }
    
    @Override
    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new GroupNodeTrackingStrategy();
    }
    
    @Override
    protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        GroupParameters groupParameters = (GroupParameters)parameters;
        Assert.notNull(groupParameters.propertyProvider);
        Assert.notNull(groupParameters.discoveryStrategy);
        Assert.notNull(groupParameters.stateStore);
        Assert.notNull(groupParameters.stateTransferFactory);
        Assert.notNull(groupParameters.deliveryHandler);
        Assert.notNull(groupParameters.localFlowController);
        
        GroupFactoryParameters groupFactoryParameters = (GroupFactoryParameters)factoryParameters;
        
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners = new HashSet<IPreparedGroupMembershipListener>();
        Set<IGroupMembershipListener> membershipListeners = new HashSet<IGroupMembershipListener>();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, groupParameters.propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        membershipManager = new CoreGroupMembershipManager(channelName, localNodeProvider, 
            preparedMembershipListeners, membershipListeners);

        Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
        CoreGroupFailureDetectionProtocol failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipManager, 
            failureDetectionListeners, groupFactoryParameters.failureUpdatePeriod, groupFactoryParameters.failureHistoryPeriod, 
            groupFactoryParameters.maxShunCount);
        preparedMembershipListeners.add(failureDetectionProtocol);
        failureObservers.add(failureDetectionProtocol);
        
        CoreGroupDiscoveryProtocol discoveryProtocol = new CoreGroupDiscoveryProtocol(channelName, messageFactory, membershipManager, 
            failureDetectionProtocol, groupParameters.discoveryStrategy, liveNodeProvider, groupFactoryParameters.discoveryPeriod, 
            groupFactoryParameters.groupFormationPeriod);
        preparedMembershipListeners.add(discoveryProtocol);
        membershipListeners.add(discoveryProtocol);
        membershipManager.setNodeDiscoverer(discoveryProtocol);
        
        FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, membershipManager);
        protocols.add(flowControlProtocol);
        failureDetectionListeners.add(flowControlProtocol);
        flowControlProtocol.setFailureDetector(failureDetectionProtocol);
        
        StateTransferClientProtocol stateTransferClientProtocol = new StateTransferClientProtocol(channelName,
            messageFactory, membershipManager, groupParameters.stateTransferFactory, groupParameters.stateStore, 
            serializationRegistry, groupFactoryParameters.maxStateTransferPeriod, groupFactoryParameters.stateSizeThreshold);
        protocols.add(stateTransferClientProtocol);
        discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
        failureDetectionListeners.add(stateTransferClientProtocol);
        
        StateTransferServerProtocol stateTransferServerProtocol = new StateTransferServerProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, groupParameters.stateTransferFactory, 
            groupParameters.stateStore, serializationRegistry, 
            groupFactoryParameters.saveSnapshotPeriod, groupFactoryParameters.transferLogRecordPeriod, 
            groupFactoryParameters.transferLogMessagesCount, groupFactoryParameters.minLockQueueCapacity,
            GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_GROUP_ID);
        protocols.add(stateTransferServerProtocol);
        stateTransferServerProtocol.setFlowController(flowControlProtocol);
        
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, groupFactoryParameters.maxBundlingMessageSize, 
            groupFactoryParameters.maxBundlingPeriod, 
            groupFactoryParameters.maxBundleSize, groupFactoryParameters.maxTotalOrderBundlingMessageCount, 
            groupFactoryParameters.maxUnacknowledgedPeriod, groupFactoryParameters.maxUnacknowledgedMessageCount, 
            groupFactoryParameters.maxIdleReceiveQueuePeriod, groupParameters.deliveryHandler, true, true, 
            groupFactoryParameters.maxUnlockQueueCapacity, groupFactoryParameters.minLockQueueCapacity, 
            serializationRegistry, GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_GROUP_ID);
        protocols.add(multicastProtocol);
        failureDetectionListeners.add(multicastProtocol);
        multicastProtocol.setRemoteFlowController(flowControlProtocol);
        multicastProtocol.setLocalFlowController(groupParameters.localFlowController);
        flowControlProtocol.setFlowController(multicastProtocol);
        
        List<IFlushParticipant> flushParticipants = new ArrayList<IFlushParticipant>();
        flushParticipants.add(stateTransferClientProtocol);
        flushParticipants.add(stateTransferServerProtocol);
        flushParticipants.add(multicastProtocol);
        FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
           flushParticipants, membershipManager, failureDetectionProtocol);
        protocols.add(flushParticipantProtocol);
        FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
            membershipManager, failureDetectionProtocol, groupFactoryParameters.flushTimeout, flushParticipantProtocol);
        failureDetectionListeners.add(flushCoordinatorProtocol);
        protocols.add(flushCoordinatorProtocol);

        GroupDataExchangeProtocol dataExchangeProtocol = new GroupDataExchangeProtocol(channelName, messageFactory, membershipManager,
            failureDetectionProtocol, Arrays.<IDataExchangeProvider>asList(), groupFactoryParameters.dataExchangePeriod);
        membershipListeners.add(dataExchangeProtocol);
        protocols.add(dataExchangeProtocol);
        failureDetectionListeners.add(dataExchangeProtocol);
        
        protocols.add(discoveryProtocol);
        protocols.add(failureDetectionProtocol);
        
        membershipTracker = new CoreGroupMembershipTracker(groupFactoryParameters.membershipTrackPeriod, membershipManager, discoveryProtocol, 
            failureDetectionProtocol, flushCoordinatorProtocol, groupFactoryParameters.flushCondition);
        
        gracefulExitStrategies.add(flushCoordinatorProtocol);
        gracefulExitStrategies.add(flushParticipantProtocol);
        gracefulExitStrategies.add(membershipTracker);
    }
    
    @Override
    protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
        failureDetectionProtocol.setFailureObserver(transport);
        failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
        channel.getCompartment().addTimerProcessor(membershipTracker);
        
        GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
        strategy.setFailureDetector(failureDetectionProtocol);
        strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        
        StateTransferClientProtocol stateTransferClientProtocol = protocolStack.find(StateTransferClientProtocol.class);
        stateTransferClientProtocol.setChannelReconnector((IChannelReconnector)channel);
        stateTransferClientProtocol.setCompartment(channel.getCompartment());
        
        StateTransferServerProtocol stateTransferServerProtocol = protocolStack.find(StateTransferServerProtocol.class);
        stateTransferServerProtocol.setCompartment(channel.getCompartment());
        
        FailureAtomicMulticastProtocol multicastProtocol = protocolStack.find(FailureAtomicMulticastProtocol.class);
        multicastProtocol.setCompartment(channel.getCompartment());
        channel.getCompartment().addProcessor(multicastProtocol);
    }
    
    @Override
    protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        GroupFactoryParameters groupFactoryParameters = (GroupFactoryParameters)factoryParameters;
        return new GroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment, membershipManager, gracefulExitStrategies, groupFactoryParameters.gracefulExitTimeout);
    }
}
