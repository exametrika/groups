/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;



/**
 * The {@link ITcpPacketSerializer} is used to serialize/deserialize packets.
 * 
 * @param <T> packet type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpPacketSerializer<T>
{
    /**
     * Serializes packet to byte array.
     *
     * @param packet packet to serialize.
     * @return byte array
     */
    TcpPacket serialize(T packet);
    
    /**
     * Deserializes packet from byte array.
     *
     * @param buffer byte array
     * @return deserialized packet
     */
    T deserialize(TcpPacket buffer);
}
