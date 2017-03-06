/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Map;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;

/**
 * The {@link WorkerToCoreMembershipProvider} is an implementation of worker to core node membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipProvider implements IClusterMembershipProvider
{
    @Override
    public Map<String, IClusterMembershipElementDelta> getDeltas(Map<String, IClusterMembershipElement> membership)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmptyMembership(IClusterMembershipElement membership)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership,
        IClusterMembershipElementDelta delta, boolean full, boolean publicPart)
    {
        // TODO Auto-generated method stub
        return null;
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
