/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;


/**
 * The {@link ISerializationRegistrar} is a registrar of serializers in serialization registry.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISerializationRegistrar
{
    /**
     * Registers registrar's serializers in specified serialization registry.
     *
     * @param registry serialization registry
     */
    void register(ISerializationRegistry registry);
    
    /**
     * Unregisters registrar's serializers in specified serialization registry.
     *
     * @param registry serialization registry
     */
    void unregister(ISerializationRegistry registry);
}
