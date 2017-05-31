/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.io.File;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.spi.groups.cluster.state.IAsyncStateStore;

public final class TestLoadStateStore implements IAsyncStateStore
{
    private final ByteArray buffer;
    private ByteArray savedBuffer;
    
    public TestLoadStateStore(ByteArray buffer)
    {
        Assert.notNull(buffer);
        
        this.buffer = buffer;
    }
    
    public ByteArray getBuffer()
    {
        return buffer;
    }
    
    public ByteArray getSavedBuffer()
    {
        return savedBuffer;
    }
    
    @Override
    public boolean load(UUID id, File state)
    {
        if (id.equals(GroupMemberships.CORE_GROUP_ID))
            Files.writeBytes(state, buffer);
        else
            Assert.error();
        
        return true;
    }

    @Override
    public void save(UUID id, File state)
    {
        if (id.equals(GroupMemberships.CORE_GROUP_ID))
            savedBuffer = Files.readBytes(state);
        else
            Assert.error();
    }
}