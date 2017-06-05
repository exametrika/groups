/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.groups.cluster.state.IAsyncStateStore;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

public final class TestLoadStateTransferFactory implements IStateTransferFactory
{
    private final IAsyncStateStore stateStore;
    private ByteArray state;
    
    public TestLoadStateTransferFactory(IAsyncStateStore stateStore)
    {
        this.stateStore = stateStore;
    }
    
    public ByteArray getState()
    {
        return state;
    }
    
    public void setState(ByteArray state)
    {
        Assert.notNull(state);
        
        this.state = state;
    }
    
    @Override
    public IAsyncStateTransferServer createServer(UUID groupId)
    {
        return new TestLoadStateTransferServer(this);
    }

    @Override
    public IAsyncStateTransferClient createClient(UUID groupId)
    {
        return new TestLoadStateTransferClient(this);
    }

    @Override
    public IStateStore createStore(UUID groupId)
    {
        return stateStore;
    }
}