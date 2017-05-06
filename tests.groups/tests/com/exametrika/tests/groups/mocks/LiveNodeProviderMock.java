/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;

public class LiveNodeProviderMock implements ILiveNodeProvider
{
    public IAddress localNode = new UnicastAddress(UUID.randomUUID(), "test");
    
    @Override
    public long getId()
    {
        return 0;
    }

    @Override
    public IAddress getLocalNode()
    {
        return localNode;
    }

    @Override
    public List<IAddress> getLiveNodes()
    {
        return null;
    }

    @Override
    public boolean isLive(IAddress node)
    {
        return false;
    }

    @Override
    public IAddress findById(UUID id)
    {
        return null;
    }

    @Override
    public IAddress findByName(String name)
    {
        return null;
    }

    @Override
    public IAddress findByConnection(String connection)
    {
        return null;
    }
}