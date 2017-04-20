/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.util.UUID;

import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.ISimpleStateStore;

/**
 * The {@link EmptySimpleStateStore} represents an empty simple state store.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class EmptySimpleStateStore implements ISimpleStateStore
{
    @Override
    public ByteArray load(UUID id)
    {
        return null;
    }

    @Override
    public void save(UUID id, ByteArray state)
    {
    }
}