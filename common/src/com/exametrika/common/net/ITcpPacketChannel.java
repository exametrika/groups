/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link ITcpPacketChannel} is a TCP packet-oriented channel.
 * 
 * @param <T> packet type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpPacketChannel<T> extends ITcpChannel
{
    /**
     * Initialization parameters.
     * @param <T> packet type
     */
    public class Parameters<T> extends ITcpChannel.Parameters
    {
        /** Maximal size of packet handled by this channel. 0 means unlimited. */
        public int maxPacketSize;
        /** Packet serializer. If null packet must have type {@link ByteArray}. */
        public ITcpPacketSerializer<T> packetSerializer;
        /** Channel handshaker. Can be <c>null<c> if not set. */
        public ITcpChannelHandshaker<T> channelHandshaker;
        /** If true deletion of sent files is disabled. */
        public boolean disableFileDeletion;
    }

    /**
     * Reads packet from channel. 
     *
     * @return packet or null if entire packet is not available
     * @threadsafety Implementation of this method is not thread safe. Must be called from {@link ITcpChannelReader#onRead},
     * {@link ITcpChannelHandshaker#handshake} or {@link ITcpChannelHandshaker#disconnect} only.
     */
    T read();

    /**
     * Writes specified packet to channel.
     *
     * @param packet packet to write
     * @return true if packet can be written to channel, false if packet can not be written to channel yet
     * @threadsafety Implementation of this method is not thread safe. Must be called from {@link ITcpChannelWriter#onWrite},
     * {@link ITcpChannelHandshaker#handshake} or {@link ITcpChannelHandshaker#disconnect} only.
     */
    boolean write(T packet);
}
