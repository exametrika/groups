/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import java.util.UUID;

import com.exametrika.common.utils.InvalidArgumentException;



/** * The {@link ISerializationRegistry} maintains registry of class serializers.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISerializationRegistry
{
    /**
     * The {@link ISerializationInfo} represents serialization info, which binds serializer to serialization identifier and 
     * serialization class.
     * 
     * @author Medvedev-A
     */
    public interface ISerializationInfo
    {
        /**
         * Returns serialization identifier.
         *
         * @return serialization identifier
         */
        UUID getId();
        
        /**
         * Returns serializer used to serialize/deserialize objects of serializable class.
         *
         * @return serializer used to serialize/deserialize objects of serializable class
         */
        ISerializer getSerializer();
    }
    
    /**
     * Returns serialization info for specified identifier.
     *
     * @param id serializer identifier
     * @return serialization info
     * @exception InvalidArgumentException if serialization info is not found
     */
    ISerializationInfo getInfo(UUID id);
    
    /**
     * Returns serialization info for serializable class.
     *
     * @param serializableClass serializable class
     * @return serialization info
     * @exception InvalidArgumentException if serialization info is not found
     */
    ISerializationInfo getInfo(Class<?> serializableClass);

    /**
     * Registers registrar.
     *
     * @param registrar registrar being registered
     */
    void register(ISerializationRegistrar registrar);
    
    /**
     * Registers serializer.
     *
     * @param id serialization identifier
     * @param serializableClass serializable class
     * @param serializer serializer
     * @exception InvalidArgumentException if id or serializableClass being registered are not unique in this registry 
     */
    void register(UUID id, Class<?> serializableClass, ISerializer serializer);
    
    /**
     * Unregisters registrar.
     *
     * @param registrar registrar being unregistered
     */
    void unregister(ISerializationRegistrar registrar);
    
    /**
     * Unregisters serializer.
     *
     * @param id serializer identifier being unregistered
     */
    void unregister(UUID id);
}
