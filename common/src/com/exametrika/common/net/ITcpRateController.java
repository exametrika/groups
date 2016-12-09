/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;



/**
 * The {@link ITcpRateController} is a channel rate controller.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface ITcpRateController
{
    /**
     * Called when timer has been elapsed.
     *
     * @param currentTime current time
     */
    void onTimer(long currentTime);
    
    /**
     * Can client read from channel?
     *
     * @return true if client can read from channel
     */
    boolean canRead();
    
    /**
     * Can client write to channel?
     *
     * @return true if client can write to channel
     */
    boolean canWrite();
    
    /**
     * Increments read byte count of channel in order to calculate current byte rate of the channel.
     *
     * @param readCount number of bytes has been read from the channel
     */
    void incrementReadCount(long readCount);
    
    /**
     * Increments write byte count of channel in order to calculate current byte rate of the channel.
     *
     * @param writeCount number of bytes has been written to the channel
     */
    void incrementWriteCount(long writeCount);
}
