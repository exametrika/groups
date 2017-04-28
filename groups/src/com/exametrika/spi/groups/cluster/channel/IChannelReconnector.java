/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.channel;



/**
 * The {@link IChannelReconnector} is used to forcefully stop the channel if group has erroneously excluded local node.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IChannelReconnector
{
    /**
     * Asynchronously closes connection to cluster with leave reason 
     * {@link com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason#RECONNECT}. 
     * Called when local node has detected that it was excluded from cluster.
     */
    void reconnect();
}
