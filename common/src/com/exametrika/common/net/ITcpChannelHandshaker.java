/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpChannelHandshaker} represents custom handshake procedure between two packet channel endpoints.
 * 
 * @param <T> packet type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpChannelHandshaker<T>
{
    /**
     * Called when handshake procedure is in progress.
     * 
     * @param channel handshaking channel
     * @return true if handshake completed sucessfully, false - if handshake must be continued
     */
    boolean handshake(ITcpPacketChannel<T> channel);
    
    /**
     * Checks if packet is disconnect packet.
     *
     * @param packet packet to check
     * @return true if packet is request to disconnect packet, false if packet is ordinary packet
     */
    boolean canDisconnect(T packet);
    
    /**
     * Called when disconnect procedure in progress. Because disconnecting procedure is asynchronous, disconnecting channel 
     * can contain ordinary packets, which may be skipped by handshaker till the first packet of disconnect handshake has been received.
     * 
     * @param channel disconnecting channel
     * @return true if disconnect completed sucessfully, false - if disconnect must be continued
     */
    boolean disconnect(ITcpPacketChannel<T> channel);
}
