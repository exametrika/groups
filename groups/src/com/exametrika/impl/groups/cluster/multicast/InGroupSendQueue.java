/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;


/**
 * The {@link InGroupSendQueue} is an in-group send queue of durable failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class InGroupSendQueue extends AbstractSendQueue
{
    private final IGroupFailureDetector failureDetector;
    private boolean groupFormed;
    
    public InGroupSendQueue(ISender sender, IGroupFailureDetector failureDetector, ITimeService timeService, 
        IDeliveryHandler deliveryHandler, boolean durable, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IMessageFactory messageFactory, GroupAddress groupAddress, UUID groupId,
        ILogger logger, IMarker marker)
    {
        super(sender, timeService, deliveryHandler, durable, maxUnlockQueueCapacity, minLockQueueCapacity, messageFactory, 
            groupAddress, groupId, logger, marker);

        Assert.notNull(failureDetector);
        
        this.failureDetector = failureDetector;
    }

    public void beforeProcessFlush(IFlush flush)
    {
        Set<INode> nodes = new HashSet<INode>(flush.getNewMembership().getGroup().getMembers());
        nodes.removeAll(failureDetector.getFailedMembers());
        nodes.removeAll(failureDetector.getLeftMembers());
        
        acknowledgedMessageIds.clear();
        for (INode node : nodes)
            acknowledgedMessageIds.put(node.getAddress(), lastCompletedMessageId);
    }
    
    public void endFlush()
    {
        groupFormed = true;
    }
    
    @Override
    protected boolean canWrite()
    {
        return groupFormed && super.canWrite();
    }
}

