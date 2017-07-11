/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import com.exametrika.common.messaging.IAddress;


/**
 * The {@link IFailureAtomicMulticast} represents a failure atomic multicast.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFailureAtomicMulticast
{
    /**
     * Tries to grant flush.
     */
    void tryGrantFlush();
    
    /**
     * Ensures receive queue.
     *
     * @param sender sender
     * @param startMessageId start message identifier.
     * @return receive queue
     */
    ReceiveQueue ensureReceiveQueue(IAddress sender, long startMessageId);
}