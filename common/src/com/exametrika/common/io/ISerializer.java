/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import java.util.UUID;

/**
 * The {@link ISerializer} is used to serialize/deserialize an object.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISerializer
{
    /**
     * Serializes an object.
     *
     * @param serialization helper object to serialize to
     * @param object object being serialized
     */
    void serialize(ISerialization serialization, Object object);
    
    /**
     * Deserializes an object.
     *
     * @param deserialization helper object to deserialize from
     * @param id serialization identifier of object being deserialized
     * @return deserialized object
     */
    Object deserialize(IDeserialization deserialization, UUID id);
}
