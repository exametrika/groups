/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.Set;
import java.util.TreeSet;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.impl.groups.cluster.discovery.ICoreNodeDiscoverer;

public class NodeDiscovererMock implements ICoreNodeDiscoverer
{
    public boolean canFormGroup;
    public Set<INode> discoveredNodes = new TreeSet<INode>();
    public boolean startDiscovery;
    
    @Override
    public void startDiscovery()
    {
        startDiscovery = true;
    }

    @Override
    public boolean canFormGroup()
    {
        return canFormGroup;
    }

    @Override
    public Set<INode> getDiscoveredNodes()
    {
        return discoveredNodes;
    }
}