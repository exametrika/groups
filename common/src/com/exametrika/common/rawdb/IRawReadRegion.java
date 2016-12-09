/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link IRawReadRegion} represents a read region interface.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawReadRegion
{
    /**
     * Returns region offset from the beginning of page.
     * @return region offset from the beginning of page
     */
    int getOffset();
    
    /**
     * Returns length.
     *
     * @return length
     */
    int getLength();
    
    /**
     * Is region read-only?
     *
     * @return true if region is read-only
     */
    boolean isReadOnly();
    
    /**
     * Is region contains native non-heap memory?
     *
     * @return true if region contains native non-heap memory
     */
    boolean isNative();
    
    /**
     * Returns region's parent.
     *
     * @return region's parent if this region is subregion of parent or null if this region is top-level region
     */
    <T extends IRawReadRegion> T getParent();
    
    /**
     * Returns sub-region.
     *
     * @param offset sub-region offset
     * @param length sub-region length 
     * @return sub-region
     */
    <T extends IRawReadRegion> T getRegion(int offset, int length);
    
    /**
     * Reads byte value.
     *
     * @param index region index
     * @return byte value
     */
    byte readByte(int index);
    
    /**
     * Reads byte array value.
     *
     * @param index region index
     * @param length array length
     * @return byte array value
     */
    ByteArray readByteArray(int index, int length);

    /**
     * Reads byte array value.
     *
     * @param index region index
     * @param value byte array value
     * @param offset byte array offset
     * @param length array length
     */
    void readByteArray(int index, byte[] value, int offset, int length);

    /**
     * Reads char value.
     *
     * @param index region index
     * @return char value
     */
    char readChar(int index);
    
    /**
     * Reads char array value.
     *
     * @param index region index
     * @param value char array value
     * @param offset char array offset
     * @param length array length
     */
    void readCharArray(int index, char[] value, int offset, int length);
    
    /**
     * Reads string value.
     *
     * @param index region index
     * @param length array length
     * @return string value
     */
    String readString(int index, int length);
    
    /**
     * Reads short value.
     *
     * @param index region index
     * @return short value
     */
    short readShort(int index);
    
    /**
     * Reads short array value.
     *
     * @param index region index
     * @param value short array value
     * @param offset short array offset
     * @param length array length
     */
    void readShortArray(int index, short[] value, int offset, int length);
    
    /**
     * Reads int value.
     *
     * @param index region index
     * @return int value
     */
    int readInt(int index);
    
    /**
     * Reads int array value.
     *
     * @param index region index
     * @param value int array value
     * @param offset int array offset
     * @param length array length
     */
    void readIntArray(int index, int[] value, int offset, int length);
    
    /**
     * Reads long value.
     *
     * @param index region index
     * @return long value
     */
    long readLong(int index);
    
    /**
     * Reads long array value.
     *
     * @param index region index
     * @param value long array value
     * @param offset long array offset
     * @param length array length
     */
    void readLongArray(int index, long[] value, int offset, int length);
    
    /**
     * Reads float value.
     *
     * @param index region index
     * @return float value
     */
    float readFloat(int index);
    
    /**
     * Reads float array value.
     *
     * @param index region index
     * @param value float array value
     * @param offset float array offset
     * @param length array length
     */
    void readFloatArray(int index, float[] value, int offset, int length);
    
    /**
     * Reads double value.
     *
     * @param index region index
     * @return double value
     */
    double readDouble(int index);
    
    /**
     * Reads double array value.
     *
     * @param index region index
     * @param value double array value
     * @param offset double array offset
     * @param length array length
     */
    void readDoubleArray(int index, double[] value, int offset, int length);
}
