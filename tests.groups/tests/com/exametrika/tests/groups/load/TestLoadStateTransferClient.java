/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.io.File;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferClient;

public final class TestLoadStateTransferClient implements IAsyncStateTransferClient
{
    private final TestLoadStateTransferFactory factory;
    
    public TestLoadStateTransferClient(TestLoadStateTransferFactory factory)
    {
        Assert.notNull(factory);
        
        this.factory = factory;
    }
    
    @Override
    public void loadSnapshot(boolean full, File file)
    {
        factory.setState(Files.readBytes(file));
    }
}