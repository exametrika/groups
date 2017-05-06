/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;

public class FailureDetectionListenerMock implements IFailureDetectionListener
{
    public Set<INode> failedMembers = new HashSet<INode>();
    public Set<INode> leftMembers = new HashSet<INode>();
    
    @Override
    public void onMemberFailed(INode member)
    {
        failedMembers.add(member);
    }

    @Override
    public void onMemberLeft(INode member)
    {
        leftMembers.add(member);
    }
}