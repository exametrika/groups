/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.impl.groups.cluster.failuredetection.IClusterFailureDetector;

public class ClusterFailureDetectorMock implements IClusterFailureDetector
{
    public Set<INode> failedNodes = new HashSet<INode>();
    public Set<INode> leftNodes = new HashSet<INode>();
    
    @Override
    public Set<INode> takeFailedNodes()
    {
        Set<INode> result = failedNodes;
        failedNodes = new LinkedHashSet<INode>();
        return result;
    }

    @Override
    public Set<INode> takeLeftNodes()
    {
        Set<INode> result = leftNodes;
        leftNodes = new LinkedHashSet<INode>();
        return result;
    }
}