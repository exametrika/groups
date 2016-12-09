/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.util.UUID;

import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;



/**
 * The {@link ISerializationRegistryExtension} is an extension to {@link SerializationRegistry} that allows to support
 * different types of serialization.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISerializationRegistryExtension
{
    /**
     * Returns serialization info for specified identifier.
     *
     * @param id serializer identifier
     * @return serialization info or null if info is not found
     */
    ISerializationInfo findInfo(UUID id);
    
    /**
     * Returns serialization info for serializable class.
     *
     * @param serializableClass serializable class
     * @return serialization info or null if info is not found
     */
    ISerializationInfo findInfo(Class<?> serializableClass);
}
