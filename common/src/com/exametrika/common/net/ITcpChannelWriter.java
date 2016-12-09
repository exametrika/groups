/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpChannelWriter} listens to TCP channel write events.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpChannelWriter
{
    /**
     * Can writer write to specified channel?
     *
     * @param channel channel to write
     * @return if true - writer can write to specified channel, if false - writer can not write to specified channel
     */
    boolean canWrite(ITcpChannel channel);
    
    /**
     * Called when channel can write data.
     * 
     * @param channel channel to write
     */
    void onWrite(ITcpChannel channel);
}
