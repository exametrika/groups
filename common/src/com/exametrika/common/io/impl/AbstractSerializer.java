/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.util.UUID;

import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AbstractSerializer} is an abstract implementation of {@link ISerializer}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractSerializer implements ISerializationRegistrar, ISerializer
{
    private final UUID id;
    private final Class<?> serializableClass;

    /**
     * Creates a new object.
     *
     * @param id serializer identifier
     * @param serializableClass serializable class
     */
    public AbstractSerializer(UUID id, Class<?> serializableClass)
    {
        Assert.notNull(id);
        Assert.notNull(serializableClass);
        
        this.id = id;
        this.serializableClass = serializableClass;
    }
    
    /**
     * Returns serializer identifier.
     *
     * @return serializer identifier
     */
    public UUID getId()
    {
        return id;
    }
    
    /**
     * Returns serializable class.
     *
     * @return serializable class
     */
    public Class<?> getSerializableClass()
    {
        return serializableClass;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        Assert.notNull(registry);
        
        registry.register(id, serializableClass, this);
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        Assert.notNull(registry);
        
        registry.unregister(id);
    }
}
