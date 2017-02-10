/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.discovery.INodeDiscoverer;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlushCondition;
import com.exametrika.impl.groups.core.flush.IFlushManager;
import com.exametrika.impl.groups.core.membership.Memberships.MembershipDeltaInfo;

/**
 * The {@link MembershipTracker} tracks group membership and initiates installation of new group membership.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class MembershipTracker implements ICompartmentProcessor, IGracefulCloseStrategy
{
    private final long trackPeriod;
    private final IMembershipManager membershipManager;
    private final INodeDiscoverer nodeDiscoverer;
    private final IFailureDetector failureDetector;
    private final IFlushManager flushManager;
    private final IFlushCondition flushCondition;
    private long lastTrackTime;
    private boolean stopped;

    public MembershipTracker(long trackPeriod, IMembershipManager membershipManager, INodeDiscoverer nodeDiscoverer, 
        IFailureDetector failureDetector, IFlushManager flushManager, IFlushCondition flushCondition)
    {
        Assert.notNull(membershipManager);
        Assert.notNull(nodeDiscoverer);
        Assert.notNull(failureDetector);
        Assert.notNull(flushManager);

        this.trackPeriod = trackPeriod;
        this.membershipManager = membershipManager;
        this.nodeDiscoverer = nodeDiscoverer;
        this.failureDetector = failureDetector;
        this.flushManager = flushManager;
        this.flushCondition = flushCondition;
    }

    @Override
    public boolean requestClose()
    {
        stopped = true;
        return true;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (stopped)
            return;
        
        if (lastTrackTime != 0 && currentTime < lastTrackTime + trackPeriod)
            return;

        lastTrackTime = currentTime;

        if (membershipManager.getPreparedMembership() == null && !nodeDiscoverer.canFormGroup())
            return;
        if (flushManager.isFlushInProgress())
            return;

        INode currentCoordinator = failureDetector.getCurrentCoordinator();
        if (currentCoordinator != null && !currentCoordinator.equals(membershipManager.getLocalNode()))
            return;
        IMembership oldMembership = membershipManager.getMembership();
        
        if (oldMembership == null)
        {
            IMembership newMembership = Memberships.createMembership(membershipManager.getLocalNode(), nodeDiscoverer.getDiscoveredNodes());
            flushManager.install(newMembership, null);
        }
        else
        {
            MembershipDeltaInfo info = Memberships.createMembership(oldMembership, failureDetector.getFailedMembers(), failureDetector.getLeftMembers(), 
                nodeDiscoverer.getDiscoveredNodes(), flushCondition);
            
            if (info != null)
                flushManager.install(info.newMembership, info.membershipDelta);
        }
    }
}