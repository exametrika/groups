/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpChannelListener} listens to TCP channel events.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpChannelListener
{
    /**
     * Called when channel has been connected.
     * 
     * @param channel connected channel
     */
    void onConnected(ITcpChannel channel);
    
    /**
     * Called when channel has been gracefully disconnected.
     * 
     * @param channel disconnected channel
     */
    void onDisconnected(ITcpChannel channel);
    
    /**
     * Called when channel has been forcefully closed or failed.
     * 
     * @param channel failed channel
     */
    void onFailed(ITcpChannel channel);
}
