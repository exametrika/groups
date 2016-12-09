/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.jdk;

import java.util.UUID;

import com.exametrika.common.io.ISerializer;
import com.exametrika.common.utils.InvalidArgumentException;




/**
 * The {@link IJdkSerializationRegistry} is of JDK based serializers.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IJdkSerializationRegistry
{
    /**
     * Registers JDK serializer.
     *
     * @param id serialization identifier
     * @param classLoader class loader
     * @param serializer serializer
     * @exception InvalidArgumentException if id or class loader being registered are not unique in this registry 
     */
    void register(UUID id, ClassLoader classLoader, ISerializer serializer);
    
    /**
     * Unregisters JDK serializer.
     *
     * @param id serializer identifier being unregistered
     */
    void unregister(UUID id);
}
