/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.UUID;
import java.util.zip.CRC32;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.groups.cluster.check.IGroupStateChecksumProvider;
import com.exametrika.spi.groups.cluster.state.IAsyncStateStore;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

public final class TestLoadStateTransferFactory implements IStateTransferFactory, IGroupStateChecksumProvider
{
    private final IAsyncStateStore stateStore;
    private ByteArray state;
    private long checksum;
    
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
        
        CRC32 crc = new CRC32();
        crc.update(state.getBuffer(), state.getOffset(), state.getLength());
        checksum = crc.getValue();
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

    @Override
    public long getStateChecksum()
    {
        return checksum;
    }
}