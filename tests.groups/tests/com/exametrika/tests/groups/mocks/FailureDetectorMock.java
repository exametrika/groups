/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;

public class FailureDetectorMock implements IGroupFailureDetector
{
    public INode currentCoordinator;
    public List<INode> healthyNodes =new ArrayList<INode>();
    public Set<INode> failedNodes = new HashSet<INode>();
    public Set<INode> leftNodes = new HashSet<INode>();
    
    @Override
    public INode getCurrentCoordinator()
    {
        return currentCoordinator;
    }

    @Override
    public List<INode> getHealthyMembers()
    {
        return healthyNodes;
    }

    @Override
    public Set<INode> getFailedMembers()
    {
        return failedNodes;
    }

    @Override
    public Set<INode> getLeftMembers()
    {
        return leftNodes;
    }

    @Override
    public void addFailedMembers(Set<UUID> memberIds)
    {
    }

    @Override
    public void addLeftMembers(Set<UUID> memberIds)
    {
    }

    @Override
    public boolean isHealthyMember(UUID memberId)
    {
        for (INode node : healthyNodes)
        {
            if ( node.getId().equals(memberId))
                return true;
        }
        return false;
    }
}