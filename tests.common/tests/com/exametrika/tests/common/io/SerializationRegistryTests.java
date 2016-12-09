/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.io;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializer;
import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.io.jdk.JdkSerializationRegistryExtension;
import com.exametrika.common.io.jdk.JdkSerializer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link SerializationRegistryTests} are tests for {@link SerializationRegistry}.
 * 
 * @see SerializationRegistry
 * @author Medvedev-A
 */
public class SerializationRegistryTests
{
    @Test
    public void testRegister() throws Throwable
    {
        final SerializationRegistry registry = new SerializationRegistry();
        final AbstractSerializer serializer = new AbstractSerializer(UUID.randomUUID(), List.class)
        {
            @Override
            public void serialize(ISerialization serialization, Object object)
            {
            }
            
            @Override
            public Object deserialize(IDeserialization deserialization, UUID id)
            {
                return null;
            }
        };
        
        registry.register(serializer);
        
        ISerializationInfo info1 = registry.getInfo(serializer.getSerializableClass());
        ISerializationInfo info2 = registry.getInfo(serializer.getId());
        assertThat(info1 == info2, is(true));
        assertThat(info1.getSerializer() == serializer, is(true));
        assertThat(info1.getId() == serializer.getId(), is(true));
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.register(serializer);
            }
        });
        
        registry.unregister(serializer.getId());
        
        registry.register(serializer.getId(), serializer.getSerializableClass(), serializer);
        
        info1 = registry.getInfo(serializer.getSerializableClass());
        info2 = registry.getInfo(serializer.getId());
        assertThat(info1 == info2, is(true));
        assertThat(info1.getSerializer() == serializer, is(true));
        assertThat(info1.getId() == serializer.getId(), is(true));
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.register(serializer.getId(), Object.class, serializer);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.register(UUID.randomUUID(), List.class, serializer);
            }
        });
        
        registry.unregister(serializer.getId());
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.getInfo(serializer.getId());
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.getInfo(serializer.getSerializableClass());
            }
        });
    }
    
    @Test
    public void testExtension() throws Throwable
    {
        final JdkSerializationRegistryExtension extension = new JdkSerializationRegistryExtension();
        final ISerializer serializer = new JdkSerializer(null);
        final UUID id = UUID.randomUUID();
        extension.register(id, null, serializer);
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                extension.register(id, new ClassLoader(){}, serializer);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                extension.register(UUID.randomUUID(), null, serializer);
            }
        });
        
        final SerializationRegistry registry = new SerializationRegistry(extension);
        
        ISerializationInfo info = registry.getInfo(ArrayList.class);
        assertThat(serializer == info.getSerializer(), is(true));
        assertThat(info.getId(), is(id));
        
        info = registry.getInfo(id);
        assertThat(serializer == info.getSerializer(), is(true));
        assertThat(info.getId(), is(id));
        
        extension.unregister(id);
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.getInfo(ArrayList.class);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                registry.getInfo(id);
            }
        });
    }
}
