/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.ICoreNodeChannel;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.CompositeChannelFactory;
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
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory.GroupFactoryParameters;
import com.exametrika.impl.groups.cluster.discovery.CoreGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.exchange.GroupDataExchangeProtocol;
import com.exametrika.impl.groups.cluster.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlushCondition;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
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
 * The {@link WorkerNodeChannelFactory} is a worker node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerNodeChannelFactory extends CompositeChannelFactory
{
    private ClusterMembershipManager membershipManager;
    private List<IGracefulExitStrategy> gracefulExitStrategies = new ArrayList<IGracefulExitStrategy>();
    
    public WorkerNodeChannelFactory(List<AbstractChannelFactory> subChannelFactories, int mainSubChannelIndex)
    {
        super(Arrays.asList(new WorkerGroupSubChannelFactory(), new WorkerToCoreSubChannelFactory()), 0);
    }
    
    @Override
    protected ICompositeChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        ICompartment compartment, List<IChannel> subChannels)
    {
        // TODO: parameters, membershipManager, gracefulExitStrategies
        GroupFactoryParameters groupFactoryParameters = (GroupFactoryParameters)factoryParameters;
        INodeFeedbackService feedbackService =;
        return new WorkerNodeChannel(channelName, liveNodeManager, channelObserver, subChannels, 
            subChannels.get(mainSubChannelIndex), compartment, membershipManager, gracefulExitStrategies,
            groupFactoryParameters.gracefulExitTimeout, feedbackService);
    }
}
