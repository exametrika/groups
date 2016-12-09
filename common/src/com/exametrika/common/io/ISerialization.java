/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ISerialization} represents a helper object to serialize to.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface ISerialization extends IDataSerialization
{
    /**
     * Returns underlying output stream.
     *
     * @return output stream
     */
    ByteOutputStream getStream();
    
    /**
     * Returns serialization registry.
     *
     * @return serialization registry
     */
    ISerializationRegistry getRegistry();
    
    /**
     * Sets version of current object being written.
     *
     * @param version version of object being written. Version must be in range [0..255]
     * @exception InvalidArgumentException if version is not in range [0..255]
     */
    void setVersion(int version);
    
    /**
     * Sets identity preservation.
     *
     * @param value if true preserves object identity in serialized data, if false makes a copy of each object occurence
     */
    void setPreserveIdentity(boolean value);

    /**
     * Writes object value. 
     *
     * @param value object value. Can be null
     */
    void writeObject(Object value);
    
    /**
     * Writes typed object value. Typed object is a object whose type is known at develop time not at runtime.
     *
     * @param <T> object type
     * @param value object value. Can be null
     */
    <T> void writeTypedObject(T value);
    
    /**
     * Writes typed object value. Typed object is a object whose type is known at develop time not at runtime.
     *
     * @param <T> object type
     * @param value object value. Can be null
     * @param objectClass object class
     */
    <T> void writeTypedObject(T value, Class<? super T> objectClass);
    
    /**
     * Begins write region. Regions are special serialization areas, that can be deserialized idependently of
     * main deserialization. Regions can not overlap. Regions are primarily used in on-demand deserialization of
     * particular parts of deserialized object.
     */
    void beginWriteRegion();
    
    /**
     * Ends write region.
     */
    void endWriteRegion();
    
    /**
     * Writes region data.
     *
     * @param buffer buffer of region data
     */
    void writeRegion(ByteArray buffer);
    
}

