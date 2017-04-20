/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.util.UUID;

import com.exametrika.spi.groups.IStateStore;

/**
 * The {@link EmptyStateStore} represents an empty state store.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class EmptyStateStore implements IStateStore
{
    @Override
    public boolean load(UUID id, File state)
    {
        return false;
    }

    @Override
    public void save(UUID id, File state)
    {
    }
}