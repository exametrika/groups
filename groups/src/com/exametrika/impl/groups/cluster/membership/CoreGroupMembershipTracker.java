/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.IGracefulExitStrategy;
import com.exametrika.impl.groups.cluster.discovery.ICoreNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlushCondition;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships.MembershipDeltaInfo;

/**
 * The {@link CoreGroupMembershipTracker} tracks core group membership and initiates installation of new group membership.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreGroupMembershipTracker implements ICompartmentTimerProcessor, IGracefulExitStrategy
{
    private final long trackPeriod;
    private final IGroupMembershipManager membershipManager;
    private final ICoreNodeDiscoverer nodeDiscoverer;
    private final IGroupFailureDetector failureDetector;
    private final IFlushManager flushManager;
    private final IFlushCondition flushCondition;
    private long lastTrackTime;
    private boolean stopped;

    public CoreGroupMembershipTracker(long trackPeriod, IGroupMembershipManager membershipManager, ICoreNodeDiscoverer nodeDiscoverer, 
        IGroupFailureDetector failureDetector, IFlushManager flushManager, IFlushCondition flushCondition)
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
    public boolean requestExit()
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
        IGroupMembership oldMembership = membershipManager.getMembership();
        
        if (oldMembership == null)
        {
            IGroupMembership newMembership = GroupMemberships.createMembership(GroupMemberships.CORE_GROUP_ADDRESS, membershipManager.getLocalNode(), nodeDiscoverer.getDiscoveredNodes());
            flushManager.install(newMembership, null);
        }
        else
        {
            MembershipDeltaInfo info = GroupMemberships.createMembership(oldMembership, failureDetector.getFailedMembers(), failureDetector.getLeftMembers(), 
                nodeDiscoverer.getDiscoveredNodes(), flushCondition);
            
            if (info != null)
                flushManager.install(info.newMembership, info.membershipDelta);
        }
    }
}