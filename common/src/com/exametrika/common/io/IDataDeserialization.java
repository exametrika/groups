/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import java.util.UUID;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link IDataDeserialization} represents a helper object to deserialize primitive types from.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IDataDeserialization
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
     * Reads raw byte array value.
     *
     * @param length length
     * @return byte array value
     */
    ByteArray read(int length);
    
    /**
     * Reads byte value.
     *
     * @return byte value
     */
    byte readByte();
    
    /**
     * Reads char value.
     *
     * @return char value
     */
    char readChar();
    
    /**
     * Reads short value.
     *
     * @return short value
     */
    short readShort();
    
    /**
     * Reads int value.
     *
     * @return int value
     */
    int readInt();
    
    /**
     * Reads long value.
     *
     * @return long value
     */
    long readLong();
    
    /**
     * Reads boolean value.
     *
     * @return boolean value
     */
    boolean readBoolean();
    
    /**
     * Reads float value.
     *
     * @return float value
     */
    float readFloat();
    
    /**
     * Reads double value.
     *
     * @return double value
     */
    double readDouble();
    
    /**
     * Reads byte array value.
     *
     * @return byte array value
     */
    ByteArray readByteArray();
    
    /**
     * Reads string value.
     *
     * @return string value
     */
    String readString();
}
