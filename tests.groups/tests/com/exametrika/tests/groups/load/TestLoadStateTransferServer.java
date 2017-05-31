/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.io.File;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;

public final class TestLoadStateTransferServer implements IAsyncStateTransferServer
{
    private final TestLoadStateTransferFactory factory;
    
    public TestLoadStateTransferServer(TestLoadStateTransferFactory factory)
    {
        Assert.notNull(factory);
        
        this.factory = factory;
    }
    
    @Override
    public MessageType classifyMessage(IMessage message)
    {
        if (message.getPart() instanceof TestLoadMessagePart)
            return MessageType.STATE_WRITE;
        else
            return MessageType.NON_STATE;
    }

    @Override
    public void saveSnapshot(boolean full, File file)
    {
        if (factory.getState() != null)
            Files.writeBytes(file, factory.getState());
    }
}