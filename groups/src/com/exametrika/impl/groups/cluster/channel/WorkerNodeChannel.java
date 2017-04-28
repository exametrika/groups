/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.IWorkerNodeChannel;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.INodeState;
import com.exametrika.impl.groups.cluster.feedback.NodeState;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

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
        List<IChannel> subChannels, IChannel mainSubChannel, ICompartment compartment, ClusterMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout, INodeFeedbackService feedbackService,
        IChannelReconnector channelReconnector)
    {
        super(channelName, liveNodeManager, channelObserver, subChannels, mainSubChannel, compartment, membershipManager, 
            gracefulExitStrategies, gracefulExitTimeout, channelReconnector);
        
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
