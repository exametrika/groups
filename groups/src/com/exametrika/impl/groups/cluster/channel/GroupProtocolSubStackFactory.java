/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.impl.CompositeDeliveryHandler;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.GroupDefinitionStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupProtocolSubStack;
import com.exametrika.impl.groups.cluster.membership.IGroupProtocolSubStackFactory;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.state.CompositeSimpleStateTransferFactory;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferServerProtocol;
import com.exametrika.spi.groups.ISimpleStateTransferFactory;

/**
 * The {@link GroupProtocolSubStackFactory} is a worker group protocol sub-stack factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupProtocolSubStackFactory implements IGroupProtocolSubStackFactory
{
    @Override
    public GroupProtocolSubStack createProtocolSubStack(IGroup group)
    {
        FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, membershipManager);
        protocols.add(flowControlProtocol);
        failureDetectionListeners.add(flowControlProtocol);
        flowControlProtocol.setFailureDetector(failureDetectionProtocol);
        
        ISimpleStateTransferFactory stateTransferFactory = new CompositeSimpleStateTransferFactory(Arrays.asList(
            new ClusterMembershipStateTransferFactory(clusterMembershipManager, membershipProviders), 
            new GroupDefinitionStateTransferFactory(groupMappingStrategy)));
        
        SimpleStateTransferClientProtocol stateTransferClientProtocol = new SimpleStateTransferClientProtocol(channelName,
            messageFactory, membershipManager, stateTransferFactory, groupParameters.stateStore);
        protocols.add(stateTransferClientProtocol);
        discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
        failureDetectionListeners.add(stateTransferClientProtocol);
        
        SimpleStateTransferServerProtocol stateTransferServerProtocol = new SimpleStateTransferServerProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, stateTransferFactory, 
            groupParameters.stateStore, groupFactoryParameters.saveSnapshotPeriod);
        protocols.add(stateTransferServerProtocol);
        
        FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
            messageFactory, membershipManager, failureDetectionProtocol, groupFactoryParameters.maxBundlingMessageSize, 
            groupFactoryParameters.maxBundlingPeriod, 
            groupFactoryParameters.maxBundleSize, groupFactoryParameters.maxTotalOrderBundlingMessageCount, 
            groupFactoryParameters.maxUnacknowledgedPeriod, groupFactoryParameters.maxUnacknowledgedMessageCount, 
            groupFactoryParameters.maxIdleReceiveQueuePeriod, new CompositeDeliveryHandler(
                Arrays.<IDeliveryHandler>asList(commandManager, groupParameters.deliveryHandler)), true, true, 
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
        // TODO Auto-generated method stub
        return null;
    }
}
