/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.check.GroupCheckStateProtocol;
import com.exametrika.impl.groups.cluster.check.IGroupStateHashProvider;
import com.exametrika.impl.groups.cluster.discovery.WorkerGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.WorkerGroupStateUpdater;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupProtocolSubStack;
import com.exametrika.impl.groups.cluster.membership.IGroupProtocolSubStackFactory;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LastGroupProtocol;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.state.AsyncStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.AsyncStateTransferServerProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferServerProtocol;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link GroupProtocolSubStackFactory} is a worker group protocol sub-stack factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupProtocolSubStackFactory implements IGroupProtocolSubStackFactory
{
    private final String channelName;
    private final IMessageFactory messageFactory;
    private GroupFeedbackProvider groupFeedbackProvider;
    private DataLossFeedbackProvider dataLossFeedbackProvider;
    private final LocalNodeProvider localNodeProvider;
    private final IClusterMembershipService clusterMembershipService;
    private IFailureObserver failureObserver;
    private final ISerializationRegistry serializationRegistry;
    private final WorkerNodeFactoryParameters factoryParameters;
    private final WorkerNodeParameters parameters;
    private ICompartment compartment;
    private IChannelReconnector channelReconnector;

    public GroupProtocolSubStackFactory(String channelName, IMessageFactory messageFactory, 
        LocalNodeProvider localNodeProvider,
        IClusterMembershipService clusterMembershipService,
        ISerializationRegistry serializationRegistry,
        WorkerNodeFactoryParameters factoryParameters, WorkerNodeParameters parameters)
    {
        Assert.notNull(channelName);
        Assert.notNull(messageFactory);
        Assert.notNull(localNodeProvider);
        Assert.notNull(clusterMembershipService);
        Assert.notNull(serializationRegistry);
        Assert.notNull(factoryParameters);
        Assert.notNull(parameters);
        
        this.channelName = channelName;
        this.messageFactory = messageFactory;
        this.localNodeProvider = localNodeProvider;
        this.clusterMembershipService = clusterMembershipService;
        this.serializationRegistry = serializationRegistry;
        this.factoryParameters = factoryParameters;
        this.parameters = parameters;
    }
    
    public void setCompartment(ICompartment compartment)
    {
        this.compartment = compartment;
    }
    
    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        this.channelReconnector = channelReconnector;
    }
    
    public void setGroupFeedbackProvider(GroupFeedbackProvider groupFeedbackProvider)
    {
        this.groupFeedbackProvider = groupFeedbackProvider;
    }

    public void setDataLossFeedbackProvider(DataLossFeedbackProvider dataLossFeedbackProvider)
    {
        this.dataLossFeedbackProvider = dataLossFeedbackProvider;
    }
    
    public void setFailureObserver(IFailureObserver failureObserver)
    {
        this.failureObserver = failureObserver;
    }

    @Override
    public GroupProtocolSubStack createProtocolSubStack(IGroup group)
    {
        List<AbstractProtocol> protocols = new ArrayList<AbstractProtocol>();
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners = new HashSet<IPreparedGroupMembershipListener>();
        Set<IGroupMembershipListener> membershipListeners = new HashSet<IGroupMembershipListener>();
       
        GroupMembershipManager membershipManager = new GroupMembershipManager(channelName, localNodeProvider, 
            preparedMembershipListeners, membershipListeners);

        List<IClusterMembershipListener> clusterMembershipListeners = new ArrayList<IClusterMembershipListener>();
        Set<IFailureDetectionListener> failureDetectionListeners = new LinkedHashSet<IFailureDetectionListener>();
        WorkerGroupFailureDetectionProtocol failureDetectionProtocol = new WorkerGroupFailureDetectionProtocol(
            channelName, messageFactory, membershipManager, failureDetectionListeners, group.getId());
        clusterMembershipListeners.add(failureDetectionProtocol);
        failureDetectionProtocol.setFailureObserver(failureObserver);
        
        WorkerGroupDiscoveryProtocol discoveryProtocol = new WorkerGroupDiscoveryProtocol(channelName, messageFactory, 
            group.getId(), membershipManager, clusterMembershipService, failureDetectionProtocol);
        preparedMembershipListeners.add(discoveryProtocol);
        membershipListeners.add(discoveryProtocol);
        
        GroupCheckStateProtocol checkStateProtocol = null;
        if (group.getOptions().contains(GroupOption.CHECK_STATE))
        {
            checkStateProtocol = new GroupCheckStateProtocol(channelName, messageFactory, membershipManager, 
                dataLossFeedbackProvider, factoryParameters.checkStatePeriod, group.getId(),
                (GroupAddress)group.getAddress(), group.getCoordinator().getDomain());
            protocols.add(checkStateProtocol);
            failureDetectionListeners.add(checkStateProtocol);
            checkStateProtocol.setFailureDetector(failureDetectionProtocol);
        }
        
        FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, membershipManager);
        protocols.add(flowControlProtocol);
        failureDetectionListeners.add(flowControlProtocol);
        flowControlProtocol.setFailureDetector(failureDetectionProtocol);
        
        List<IFlushParticipant> flushParticipants = new ArrayList<IFlushParticipant>();
        IGroupStateHashProvider stateHashProvider = null;
        if (group.getOptions().contains(GroupOption.SIMPLE_STATE_TRANSFER))
        {
            SimpleStateTransferClientProtocol stateTransferClientProtocol = new SimpleStateTransferClientProtocol(channelName,
                messageFactory, membershipManager, parameters.stateTransferFactory, group.getId());
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            
            SimpleStateTransferServerProtocol stateTransferServerProtocol = new SimpleStateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, parameters.stateTransferFactory, 
                group.getId(), factoryParameters.saveSnapshotPeriod);
            protocols.add(stateTransferServerProtocol);
            flushParticipants.add(stateTransferClientProtocol);
            flushParticipants.add(stateTransferServerProtocol);
            
            stateHashProvider = stateTransferServerProtocol;
        }
        else if (group.getOptions().contains(GroupOption.ASYNC_STATE_TRANSFER))
        {
            AsyncStateTransferClientProtocol stateTransferClientProtocol = new AsyncStateTransferClientProtocol(channelName,
                messageFactory, membershipManager, parameters.stateTransferFactory, group.getId(), serializationRegistry,
                factoryParameters.maxStateTransferPeriod, factoryParameters.stateSizeThreshold);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            stateTransferClientProtocol.setCompartment(compartment);
            stateTransferClientProtocol.setChannelReconnector(channelReconnector);
            flushParticipants.add(stateTransferClientProtocol);
            
            AsyncStateTransferServerProtocol stateTransferServerProtocol = new AsyncStateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, parameters.stateTransferFactory, serializationRegistry,
                factoryParameters.saveSnapshotPeriod,factoryParameters.transferLogRecordPeriod, factoryParameters.transferLogMessagesCount,
                factoryParameters.minLockQueueCapacity, (GroupAddress)group.getAddress(), group.getId());
            protocols.add(stateTransferServerProtocol);
            flushParticipants.add(stateTransferServerProtocol);
            stateTransferServerProtocol.setCompartment(compartment);
            stateTransferServerProtocol.setFlowController(flowControlProtocol);
            
            stateHashProvider = stateTransferServerProtocol;
        }
        else
            Assert.error();
        
        if (group.getOptions().contains(GroupOption.CHECK_STATE))
            checkStateProtocol.setStateHashProvider(stateHashProvider);
        
        boolean durable = group.getOptions().contains(GroupOption.DURABLE);
        boolean ordered = group.getOptions().contains(GroupOption.ORDERED);
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, factoryParameters.maxBundlingMessageSize, 
            factoryParameters.maxBundlingPeriod, 
            factoryParameters.maxBundleSize, factoryParameters.maxTotalOrderBundlingMessageCount, 
            factoryParameters.maxUnacknowledgedPeriod, factoryParameters.maxUnacknowledgedMessageCount, 
            factoryParameters.maxIdleReceiveQueuePeriod, parameters.deliveryHandler, durable, ordered, 
            factoryParameters.maxUnlockQueueCapacity, factoryParameters.minLockQueueCapacity, 
            serializationRegistry, (GroupAddress)group.getAddress(), group.getId());
        protocols.add(multicastProtocol);
        failureDetectionListeners.add(multicastProtocol);
        multicastProtocol.setRemoteFlowController(flowControlProtocol);
        multicastProtocol.setLocalFlowController(parameters.localFlowController);
        flowControlProtocol.setFlowController(multicastProtocol);
        
        multicastProtocol.setCompartment(compartment);
        List<ICompartmentProcessor> compartmentProcessors = new ArrayList<ICompartmentProcessor>();
        compartmentProcessors.add(multicastProtocol);
        
        flushParticipants.add(multicastProtocol);
        FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
           flushParticipants, membershipManager, failureDetectionProtocol);
        protocols.add(flushParticipantProtocol);
        FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
            membershipManager, failureDetectionProtocol, factoryParameters.flushTimeout, flushParticipantProtocol);
        failureDetectionListeners.add(flushCoordinatorProtocol);
        protocols.add(flushCoordinatorProtocol);
        
        WorkerGroupStateUpdater groupStateUpdater = new WorkerGroupStateUpdater(groupFeedbackProvider);
        flushParticipants.add(groupStateUpdater);
        
        protocols.add(discoveryProtocol);
        protocols.add(failureDetectionProtocol);
        protocols.add(new LastGroupProtocol(channelName, messageFactory, group.getId()));

        GroupProtocolSubStack groupProtocolSubStack = new GroupProtocolSubStack(channelName, messageFactory, group.getId(), protocols, membershipManager, 
            dataLossFeedbackProvider, factoryParameters.maxGroupMembershipHistorySize, clusterMembershipListeners, compartmentProcessors);
        preparedMembershipListeners.add(groupProtocolSubStack);
        return groupProtocolSubStack;
    }
}
