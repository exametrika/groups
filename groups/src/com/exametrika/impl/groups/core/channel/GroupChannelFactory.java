/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.core.IMembershipListener;
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
import com.exametrika.impl.groups.core.discovery.DiscoveryProtocol;
import com.exametrika.impl.groups.core.exchange.DataExchangeProtocol;
import com.exametrika.impl.groups.core.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.core.failuredetection.FailureDetectionProtocol;
import com.exametrika.impl.groups.core.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.core.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.core.flush.IFlushCondition;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.impl.groups.core.membership.MembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipTracker;
import com.exametrika.impl.groups.core.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.core.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.core.multicast.RemoteFlowId;
import com.exametrika.impl.groups.core.state.StateTransferClientProtocol;
import com.exametrika.impl.groups.core.state.StateTransferServerProtocol;
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
    private MembershipTracker membershipTracker;
    private MembershipManager membershipManager;
    private List<IGracefulCloseStrategy> gracefulCloseStrategies = new ArrayList<IGracefulCloseStrategy>();
    
    public static class GroupFactoryParameters extends FactoryParameters
    {
        public final long discoveryPeriod = 500;
        public final long groupFormationPeriod = 10000;
        public long failureUpdatePeriod = 500;
        public long failureHistoryPeriod = 600000;
        public int maxShunCount = 3;
        public long flushTimeout = 300000;
        public long membershipTrackPeriod = 1000;
        public long gracefulCloseTimeout = 10000;
        public long maxStateTransferPeriod = Integer.MAX_VALUE;
        public long stateSizeThreshold = 100000;// TODO: прописать остальные значения по умолчанию
        public long saveSnapshotPeriod = 1000;
        public long transferLogRecordPeriod = 1000;
        public int transferLogMessagesCount = 2;
        public int minLockQueueCapacity = 10000000;
        public long dataExchangePeriod = 200;
        public int maxBundlingMessageSize;
        public long maxBundlingPeriod;
        public int maxBundleSize;
        public int maxTotalOrderBundlingMessageCount;
        public long maxUnacknowledgedPeriod;
        public int maxUnacknowledgedMessageCount;
        public long maxIdleReceiveQueuePeriod;
        public int maxUnlockQueueCapacity;
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
            gracefulCloseTimeout *= timeMultiplier;
        }
    }
    
    public static class GroupParameters extends Parameters
    {
        public IPropertyProvider propertyProvider= new SystemPropertyProvider();
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
        
        Set<IPreparedMembershipListener> preparedMembershipListeners = new HashSet<IPreparedMembershipListener>();
        Set<IMembershipListener> membershipListeners = new HashSet<IMembershipListener>();
        membershipManager = new MembershipManager(channelName, liveNodeProvider, groupParameters.propertyProvider, 
            preparedMembershipListeners, membershipListeners);

        Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
        FailureDetectionProtocol failureDetectionProtocol = new FailureDetectionProtocol(channelName, messageFactory, membershipManager, 
            failureDetectionListeners, groupFactoryParameters.failureUpdatePeriod, groupFactoryParameters.failureHistoryPeriod, 
            groupFactoryParameters.maxShunCount);
        preparedMembershipListeners.add(failureDetectionProtocol);
        failureObservers.add(failureDetectionProtocol);
        
        DiscoveryProtocol discoveryProtocol = new DiscoveryProtocol(channelName, messageFactory, membershipManager, 
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
            groupFactoryParameters.transferLogMessagesCount, groupFactoryParameters.minLockQueueCapacity);
        protocols.add(stateTransferServerProtocol);
        stateTransferServerProtocol.setFlowController(flowControlProtocol);
        
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, groupFactoryParameters.maxBundlingMessageSize, 
            groupFactoryParameters.maxBundlingPeriod, 
            groupFactoryParameters.maxBundleSize, groupFactoryParameters.maxTotalOrderBundlingMessageCount, 
            groupFactoryParameters.maxUnacknowledgedPeriod, groupFactoryParameters.maxUnacknowledgedMessageCount, 
            groupFactoryParameters.maxIdleReceiveQueuePeriod, groupParameters.deliveryHandler, true, true, 
            groupFactoryParameters.maxUnlockQueueCapacity, groupFactoryParameters.minLockQueueCapacity, 
            serializationRegistry);
        protocols.add(multicastProtocol);
        failureDetectionListeners.add(multicastProtocol);
        multicastProtocol.setRemoteFlowController(flowControlProtocol);
        multicastProtocol.setLocalFlowController(groupParameters.localFlowController);
        flowControlProtocol.setFlowController(multicastProtocol);
        
        FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
            Arrays.<IFlushParticipant>asList(stateTransferClientProtocol, stateTransferServerProtocol, multicastProtocol),
            membershipManager, failureDetectionProtocol);
        protocols.add(flushParticipantProtocol);
        FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
            membershipManager, failureDetectionProtocol, groupFactoryParameters.flushTimeout, flushParticipantProtocol);
        failureDetectionListeners.add(flushCoordinatorProtocol);
        protocols.add(flushCoordinatorProtocol);

        DataExchangeProtocol dataExchangeProtocol = new DataExchangeProtocol(channelName, messageFactory, membershipManager,
            failureDetectionProtocol, Arrays.<IDataExchangeProvider>asList(), groupFactoryParameters.dataExchangePeriod);
        membershipListeners.add(dataExchangeProtocol);
        protocols.add(dataExchangeProtocol);
        failureDetectionListeners.add(dataExchangeProtocol);
        
        protocols.add(discoveryProtocol);
        protocols.add(failureDetectionProtocol);
        
        membershipTracker = new MembershipTracker(groupFactoryParameters.membershipTrackPeriod, membershipManager, discoveryProtocol, 
            failureDetectionProtocol, flushCoordinatorProtocol, groupFactoryParameters.flushCondition);
        
        gracefulCloseStrategies.add(flushCoordinatorProtocol);
        gracefulCloseStrategies.add(flushParticipantProtocol);
        gracefulCloseStrategies.add(membershipTracker);
    }
    
    @Override
    protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        FailureDetectionProtocol failureDetectionProtocol = protocolStack.find(FailureDetectionProtocol.class);
        failureDetectionProtocol.setFailureObserver(transport);
        failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
        channel.getCompartment().addTimerProcessor(membershipTracker);
        
        GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
        strategy.setFailureDetector(failureDetectionProtocol);
        strategy.setMembershipManager((IMembershipManager)failureDetectionProtocol.getMembersipService());
        
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
            connectionManager, compartment, membershipManager, gracefulCloseStrategies, groupFactoryParameters.gracefulCloseTimeout);
    }
}
