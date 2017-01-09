/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;

/**
 * The {@link CleanupManagerMock} is a mock implementation of {@link ICleanupManager}.
 * 
 * @author medvedev
 */
public class CleanupManagerMock implements ICleanupManager
{
    private LiveNodeManager liveNodeManager;
    
    public CleanupManagerMock(LiveNodeManager liveNodeManager)
    {
        this.liveNodeManager = liveNodeManager;
    }
    
    @Override
    public boolean canCleanup(IAddress node)
    {
        return !liveNodeManager.isLive(node);
    }
}