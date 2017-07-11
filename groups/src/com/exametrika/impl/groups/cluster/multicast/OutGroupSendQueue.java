/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.time.ITimeService;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;


/**
 * The {@link OutGroupSendQueue} is an out-group send queue of durable failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class OutGroupSendQueue extends AbstractSendQueue
{
    public OutGroupSendQueue(ISender sender, ITimeService timeService, 
        IDeliveryHandler deliveryHandler, boolean durable, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IMessageFactory messageFactory, GroupAddress groupAddress, UUID groupId,
        ILogger logger, IMarker marker)
    {
        super(sender, timeService, deliveryHandler, durable, maxUnlockQueueCapacity, minLockQueueCapacity, messageFactory,
            groupAddress, groupId, logger, marker);
    }
}

