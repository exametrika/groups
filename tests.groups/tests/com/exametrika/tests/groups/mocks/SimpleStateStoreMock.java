/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.UUID;

import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;

public class SimpleStateStoreMock implements ISimpleStateStore
{
    public ByteArray state;
    
    @Override
    public ByteArray load(UUID id)
    {
        return state;
    }

    @Override
    public void save(UUID id, ByteArray state)
    {
        this.state = state;
    }
}