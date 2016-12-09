/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link IRawWriteRegion} represents a write region interface.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawWriteRegion extends IRawReadRegion
{
    /**
     * Writes byte value
     *
     * @param index region index
     * @param value byte value
     */
    void writeByte(int index, byte value);

    /**
     * Writes byte array value.
     *
     * @param index region index
     * @param value byte array value
     */
    void writeByteArray(int index, ByteArray value);
    
    /**
     * Writes byte array value.
     *
     * @param index region index
     * @param value byte array value
     */
    void writeByteArray(int index, byte[] value);
    
    /**
     * Writes byte array value.
     *
     * @param index region index
     * @param value byte array value
     * @param offset value offset
     * @param length value length
     */
    void writeByteArray(int index, byte[] value, int offset, int length);
    
    /**
     * Writes char value.
     *
     * @param index region index
     * @param value char value
     */
    void writeChar(int index, char value);
    
    /**
     * Writes char array value.
     *
     * @param index region index
     * @param value char array value
     */
    void writeCharArray(int index, char[] value);
    
    /**
     * Writes char array value.
     *
     * @param index region index
     * @param value char array value
     * @param offset value offset
     * @param length value length
     */
    void writeCharArray(int index, char[] value, int offset, int length);
    
    /**
     * Writes string value.
     *
     * @param index region index
     * @param value string value
     */
    void writeString(int index, String value);
    
    /**
     * Writes short value.
     *
     * @param index region index
     * @param value short value
     */
    void writeShort(int index, short value);
    
    /**
     * Writes short array value.
     *
     * @param index region index
     * @param value short array value
     */
    void writeShortArray(int index, short[] value);
    
    /**
     * Writes short array value.
     *
     * @param index region index
     * @param value short array value
     * @param offset value offset
     * @param length value length
     */
    void writeShortArray(int index, short[] value, int offset, int length);
    
    /**
     * Writes int value.
     *
     * @param index region index
     * @param value int value
     */
    void writeInt(int index, int value);
    
    /**
     * Writes int array value.
     *
     * @param index region index
     * @param value int array value
     */
    void writeIntArray(int index, int[] value);
    
    /**
     * Writes int array value.
     *
     * @param index region index
     * @param value int array value
     * @param offset value offset
     * @param length value length
     */
    void writeIntArray(int index, int[] value, int offset, int length);
    
    /**
     * Writes long value.
     *
     * @param index region index
     * @param value long value
     */
    void writeLong(int index, long value);

    /**
     * Writes long array value.
     *
     * @param index region index
     * @param value char array value
     */
    void writeLongArray(int index, long[] value);
    
    /**
     * Writes long array value.
     *
     * @param index region index
     * @param value long array value
     * @param offset value offset
     * @param length value length
     */
    void writeLongArray(int index, long[] value, int offset, int length);
    
    /**
     * Writes float value.
     *
     * @param index region index
     * @param value float value
     */
    void writeFloat(int index, float value);

    /**
     * Writes float array value.
     *
     * @param index region index
     * @param value float array value
     */
    void writeFloatArray(int index, float[] value);
    
    /**
     * Writes float array value.
     *
     * @param index region index
     * @param value float array value
     * @param offset value offset
     * @param length value length
     */
    void writeFloatArray(int index, float[] value, int offset, int length);
    
    /**
     * Writes double value.
     *
     * @param index region index
     * @param value double value
     */
    void writeDouble(int index, double value);

    /**
     * Writes double array value.
     *
     * @param index region index
     * @param value double array value
     */
    void writeDoubleArray(int index, double[] value);
    
    /**
     * Writes double array value.
     *
     * @param index region index
     * @param value double array value
     * @param offset value offset
     * @param length value length
     */
    void writeDoubleArray(int index, double[] value, int offset, int length);
    
    /**
     * Fill sub-region by specified value.
     *
     * @param index sub-region index
     * @param length sub-region length
     * @param value value
     */
    void fill(int index, int length, byte value);
    
    /**
     * Copies sub-region to specified location.
     *
     * @param fromIndex source sub-region index
     * @param toIndex destination sub-region index
     * @param length sub-region length
     */
    void copy(int fromIndex, int toIndex, int length);
}
