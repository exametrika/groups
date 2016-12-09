/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link IDeserialization} represents a helper object to deserialize from.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IDeserialization extends IDataDeserialization
{
    /**
     * Returns underlying input stream.
     *
     * @return input stream
     */
    ByteInputStream getStream();
    
    /**
     * Returns serialization registry.
     *
     * @return serialization registry
     */
    ISerializationRegistry getRegistry();
    
    /**
     * Returns version of current object being read.
     *
     * @return version of object being read 
     */
    int getVersion();

    /**
     * Reads object value.
     *
     * @param <T> object type
     * @return object value
     */
    <T> T readObject();
    
    /**
     * Reads typed object value. Typed object is a object whose type is known at develop time not at runtime.
     *
     * @param <C> class type
     * @param <T> object type
     * @param objectClass class of object being read
     * @return object value
     */
    <C, T extends C> T readTypedObject(Class<C> objectClass);
    
    /**
     * Publishes reference for partially created object being read, to be used in subsequent reference resolutions. Used when 
     * identity is preserved for current object and specified reference has cyclic references from its own fields.
     * Reference must be published before any object field is read. If current object does not support identity,
     * this method has no effect.
     *
     * @param reference object reference being published
     */
    void publishReference(Object reference);
 
    /**
     * Begins read region. Regions are special serialization areas, that can be deserialized independently of
     * main deserialization. Regions can not overlap. Regions are primarily used in on-demand deserialization of
     * particular parts of deserialized object.
     *
     * @return byte array of region
     */
    ByteArray beginReadRegion();
    
    /**
     * Ends read region by moving stream position after region area. Current stream position must be within region area.
     */
    void endReadRegion();
    
    /**
     * Reads region data by extracting data buffer and moving stream position after region area. 
     * Current stream position must be within region area.
     *
     * @return region data buffer
     */
    ByteArray readRegion();
}
