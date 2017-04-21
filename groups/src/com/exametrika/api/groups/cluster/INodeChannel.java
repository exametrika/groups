/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.messaging.ICompositeChannel;

/**
 * The {@link INodeChannel} is a node channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface INodeChannel extends ICompositeChannel
{
    /**
     * Returns cluster membership service.
     *
     * @return cluster membership service
     */
    IClusterMembershipService getMembershipService();
    
    /**
     * Closes channel.
     *
     * @param gracefully if true channel is closed gracefully, else channel is closed forcefully
     */
    void close(boolean gracefully);
}