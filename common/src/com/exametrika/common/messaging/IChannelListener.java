/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;




/**
 * The {@link IChannelListener} is a channel listener.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IChannelListener
{
    /**
     * Called when new live node connected to channel.
     *
     * @param node new live node address
     */
    void onNodeConnected(IAddress node);
    
    /**
     * Called when node has failed.
     *
     * @param node address of failed node
     */
    void onNodeFailed(IAddress node);
    
    /**
     * Called when node has disconnected.
     *
     * @param node address of left node
     */
    void onNodeDisconnected(IAddress node);
}
