/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import java.util.UUID;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link IDataSerialization} represents a helper object to serialize primitive types to.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IDataSerialization
{
    /**
     * Returns serializer-specific extension.
     *
     * @param <T> extension type
     * @param id identifier of extension
     * @return extension value
     */
    <T> T getExtension(UUID id);
    
    /**
     * Sets serializer-specific extension.
     *
     * @param <T> extension type
     * @param id identifier of extension
     * @param value extension value
     */
    <T> void setExtension(UUID id, T value);

    /**
     * Writes raw byte array value.
     *
     * @param value byte array value
     */
    void write(ByteArray value);
    
    /**
     * Writes byte value
     *
     * @param value byte value
     */
    void writeByte(byte value);
    
    /**
     * Writes char value.
     *
     * @param value char value
     */
    void writeChar(char value);
    
    /**
     * Writes short value.
     *
     * @param value short value
     */
    void writeShort(short value);
    
    /**
     * Writes int value.
     *
     * @param value int value
     */
    void writeInt(int value);
    
    /**
     * Writes long value.
     *
     * @param value long value
     */
    void writeLong(long value);
    
    /**
     * Writes boolean value.
     *
     * @param value boolean value
     */
    void writeBoolean(boolean value);
    
    /**
     * Writes float value.
     *
     * @param value float value
     */
    void writeFloat(float value);
    
    /**
     * Writes double value.
     *
     * @param value double value
     */
    void writeDouble(double value);

    /**
     * Writes byte array value.
     *
     * @param value byte array value. Can be null
     */
    void writeByteArray(ByteArray value);
    
    /**
     * Writes string value.
     *
     * @param value string value. Can be null
     */
    void writeString(String value);
}

