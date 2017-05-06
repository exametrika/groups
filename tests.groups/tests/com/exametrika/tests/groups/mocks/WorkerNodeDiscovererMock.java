/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;

public class WorkerNodeDiscovererMock implements IWorkerNodeDiscoverer
{
    public Set<INode> discoveredNodes = new LinkedHashSet<INode>();
    
    @Override
    public Set<INode> takeDiscoveredNodes()
    {
        Set<INode> result = discoveredNodes;
        discoveredNodes = new LinkedHashSet<INode>();
        return result;
    }
}