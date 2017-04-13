/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.IWorkerNodeChannel;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.INodeState;
import com.exametrika.impl.groups.cluster.feedback.NodeState;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipManager;

/**
 * The {@link WorkerNodeChannel} is a worker node channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class WorkerNodeChannel extends NodeChannel implements IWorkerNodeChannel
{
    private final INodeFeedbackService feedbackService;

    public WorkerNodeChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver,
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory,
        IConnectionProvider connectionProvider, ICompartment compartment, GroupMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout, INodeFeedbackService feedbackService)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider,
            compartment, membershipManager, gracefulExitStrategies, gracefulExitTimeout);
        
        Assert.notNull(feedbackService);
        
        this.feedbackService = feedbackService;
    }
    
    @Override
    protected void doStartWaitGracefulExit()
    {
        INode localNode = membershipManager.getLocalNode();
        INodeState state = new NodeState(localNode.getDomain(), localNode.getId(), INodeState.State.GRACEFUL_EXIT_REQUESTED);
        feedbackService.updateNodeState(state);
    }
}
