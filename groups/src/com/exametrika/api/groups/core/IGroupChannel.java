/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;

import com.exametrika.common.messaging.IChannel;

/**
 * The {@link IGroupChannel} is a group channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupChannel extends IChannel
{
    /**
     * Returns membership service.
     *
     * @return membership service
     */
    IMembershipService getMembershipService();
    
    /**
     * Closes channel.
     *
     * @param gracefully if true channel is closed gracefully, else channel is closed forcefully
     */
    void close(boolean gracefully);
}