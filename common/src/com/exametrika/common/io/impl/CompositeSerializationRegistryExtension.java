/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.ISerializationRegistry.ISerializationInfo;
import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositeSerializationRegistryExtension} is a registry extension that is composition of other registry extensions.
 * 
 * @author medvedev
 */
public final class CompositeSerializationRegistryExtension implements ISerializationRegistryExtension
{
    private final List<ISerializationRegistryExtension> extensions;

    public CompositeSerializationRegistryExtension(List<ISerializationRegistryExtension> extensions)
    {
        Assert.notNull(extensions);
        this.extensions = extensions;
    }
    
    @Override
    public ISerializationInfo findInfo(UUID id)
    {
        for (ISerializationRegistryExtension extension : extensions)
        {
            ISerializationInfo info = extension.findInfo(id);
            if (info != null)
                return info;
        }
        return null;
    }

    @Override
    public ISerializationInfo findInfo(Class<?> serializableClass)
    {
        for (ISerializationRegistryExtension extension : extensions)
        {
            ISerializationInfo info = extension.findInfo(serializableClass);
            if (info != null)
                return info;
        }
        return null;
    }
}
