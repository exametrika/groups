/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.jdk;

import java.io.Serializable;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link JdkSerializer} is serializer for JDK serialization.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JdkSerializer implements ISerializer
{
    private final ClassLoader loader;
 
    /**
     * Creates a new object.
     * 
     * @param loader class loader to load classes from. Can be null
     */
    public JdkSerializer(ClassLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        Serializers.serialize(serialization.getStream(), (Serializable)object);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        return Serializers.deserialize(deserialization.getStream(), loader);
    }
}
