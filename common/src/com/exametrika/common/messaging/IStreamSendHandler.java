/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;





/**
 * The {@link IStreamSendHandler} is used to send streams.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IStreamSendHandler
{
    /**
     * Returns number of streams being sent. If stream count is 0, message is send as ordinary (not streaming message),
     * i.e. stream handler notifications are not called.
     *
     * @return number of streams
     */
    int getStreamCount();
    
    /**
     * Does handler have data to send?
     * 
     * @return true if handler has data to send
     */
    boolean hasData();
    
    /**
     * Reads next stream fragment for sending.
     *
     * @param buffer send buffer
     * @return read length or -1 if end of stream has been reached
     */
    int read(byte[] buffer);
    
    /**
     * Called when stream sending has been started.
     * 
     * @param streamIndex stream index
     */
    void sendStreamStarted(int streamIndex);
    
    /**
     * Called when stream sending has been completed.
     */
    void sendStreamCompleted();
    
    /**
     * Called when stream sending has been started.
     */
    void sendStarted();
    
    /**
     * Called when stream sending has been completed.
     */
    void sendCompleted();
    /**
     * Called when stream sending has been canceled.
     */
    void sendCanceled();
}
