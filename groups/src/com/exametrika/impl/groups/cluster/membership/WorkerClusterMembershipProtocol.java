/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.common.messaging.IMessageFactory;

/**
 * The {@link WorkerClusterMembershipProtocol} represents a worker node part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerClusterMembershipProtocol extends AbstractClusterMembershipProtocol
{
    public WorkerClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders)
    {
        super(channelName, messageFactory, membershipManager, membershipProviders);
    }
}
