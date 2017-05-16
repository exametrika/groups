/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;

public class FlowControllerMock implements IFlowController<RemoteFlowId>
{
    public List<RemoteFlowId> lockedFlows = new ArrayList<RemoteFlowId>();
    public List<RemoteFlowId> unlockedFlows = new ArrayList<RemoteFlowId>();
    
    @Override
    public void lockFlow(RemoteFlowId flow)
    {
        lockedFlows.add(flow);
    }

    @Override
    public void unlockFlow(RemoteFlowId flow)
    {
        unlockedFlows.add(flow);
    }
}