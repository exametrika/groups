/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;

/**
 * The {@link SerializationRegistryFactory} is a factory for {@link SerializationRegistry}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SerializationRegistryFactory extends AbstractSingletonComponentFactory<SerializationRegistry>
{
    public SerializationRegistryFactory()
    {
        super(true);
    }
    
    @Override
    protected SerializationRegistry createInstance()
    {
        return new SerializationRegistry();
    }
}
