/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;





/**
 * The {@link IStreamReceiveHandler} is used to receive streams.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IStreamReceiveHandler
{
    /**
     * Writes received data.
     *
     * @param buffer receive buffer
     * @param offset buffer offset
     * @param length buffer length
     */
    void write(byte[] buffer, int offset, int length);
    
    /**
     * Called when stream receiving has been started.
     * 
     * @param streamIndex stream index
     */
    void receiveStreamStarted(int streamIndex);
    
    /**
     * Called when stream receiving has been completed.
     */
    void receiveStreamCompleted();
    
    /**
     * Called when stream receiving has been started.
     * 
     * @param streamCount stream count to be received
     */
    void receiveStarted(int streamCount);
    
    /**
     * Called when stream receiving has been completed.
     */
    void receiveCompleted();
    
    /**
     * Called when stream receiving has been canceled.
     */
    void receiveCanceled();
}
