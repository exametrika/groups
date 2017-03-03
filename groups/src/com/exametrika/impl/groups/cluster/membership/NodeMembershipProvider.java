/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.IWorkerFailureDetector;

/**
 * The {@link NodeMembershipProvider} is an implementation of node membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeMembershipProvider implements IClusterMembershipProvider
{
    private final IWorkerNodeDiscoverer nodeDiscoverer;
    private final IWorkerFailureDetector failureDetector;

    public NodeMembershipProvider(IWorkerNodeDiscoverer nodeDiscoverer, IWorkerFailureDetector failureDetector)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.notNull(failureDetector);
        
        this.nodeDiscoverer = nodeDiscoverer;
        this.failureDetector = failureDetector;
    }
    
    @Override
    public boolean isCoreOnly()
    {
        return false;
    }

    @Override
    public boolean hasChanges()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IClusterMembershipElementDelta getDelta(IClusterMembershipElement membership)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IClusterMembershipElementDelta createFullDelta(IClusterMembershipElement membership)
    {
        NodeMembership nodeMembership = (NodeMembership)membership;
        return new NodeMembershipDelta(new ArrayList<INode>(nodeMembership.getNodes()), Collections.<UUID>emptySet(), 
            Collections.<UUID>emptySet());
    }

    @Override
    public IClusterMembershipElement createMembership(IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IClusterMembershipElementChange createChange(IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IClusterMembershipElementChange createChange(IClusterMembershipElement newMembership,
        IClusterMembershipElement oldMembership)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
