/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.jdk;

import java.io.Serializable;
import java.util.UUID;

import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.io.impl.ISerializationRegistryExtension;
import com.exametrika.common.utils.Assert;


/**
 * The {@link SimpleJdkSerializationRegistryExtension} is an serialization mapper that maps all {@link Serializable} objects to
 * {@link JdkSerializer}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleJdkSerializationRegistryExtension implements ISerializationRegistryExtension
{
    private static final UUID JDK_SERIALIZER_ID = UUID.fromString("1fbfe71d-8227-436e-b3cb-ff47709fdb40");
    private final ClassLoader classLoader;
    private final ISerializationInfo serializationInfo;
    
    public SimpleJdkSerializationRegistryExtension(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
        serializationInfo = new SerializationInfo(new JdkSerializer(classLoader));
    }
    
    @Override
    public ISerializationInfo findInfo(UUID id)
    {
        Assert.notNull(id);
        if (id.equals(JDK_SERIALIZER_ID))
            return serializationInfo;
        else
            return null;
    }

    @Override
    public ISerializationInfo findInfo(Class<?> serializableClass)
    {
        if (!Serializable.class.isAssignableFrom(serializableClass) || 
            (serializableClass.getClassLoader() != null && serializableClass.getClassLoader() != classLoader))
            return null;
        
        return serializationInfo;
    }
    
    private class SerializationInfo implements ISerializationInfo
    {
        private final ISerializer serializer;

        public SerializationInfo(ISerializer serializer)
        {
            this.serializer = serializer;
        }
        
        @Override
        public UUID getId()
        {
            return JDK_SERIALIZER_ID;
        }
        
        @Override
        public ISerializer getSerializer()
        {
            return serializer;
        }
    }
}
