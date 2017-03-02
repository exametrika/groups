/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlushManager;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.Memberships;

/**
 * The {@link CoreClusterMembershipProtocol} represents a core node part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterMembershipProtocol extends AbstractClusterMembershipProtocol implements IGracefulCloseStrategy
{
    private final IMembershipManager membershipManager;
    private final ISender workerSender;
    private final long trackPeriod;
    private IFailureDetector failureDetector;
    private IFlushManager flushManager;
    private long lastTrackTime;
    private boolean stopped;
    private boolean installing;
    
    public CoreClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders,
        IMembershipManager membershipManager, ISender workerSender, long trackPeriod)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders);
        
        Assert.notNull(membershipManager);
        Assert.notNull(workerSender);
        
        this.membershipManager = membershipManager;
        this.workerSender = workerSender;
        this.trackPeriod = trackPeriod;
    }
    
    public void setFailureDetector(IFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    public void setFlushManager(IFlushManager flushManager)
    {
        Assert.notNull(flushManager);
        Assert.isNull(this.flushManager);
        
        this.flushManager = flushManager;
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
        if (!canInstallMembership(currentTime))
            return;
        
        installing = true;
        
        IClusterMembership oldMembership = clusterMembershipManager.getMembership();
        List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
        for (int i= 0; i < membershipProviders.size(); i++)
        {
            IClusterMembershipElementDelta delta = membershipProviders.get(i).createDelta(
                oldMembership != null ? oldMembership.getElements().get(i) : null);
            deltas.add(delta);
        }
        
        ClusterMembershipDelta delta;
        if (oldMembership == null)
            delta = new ClusterMembershipDelta(1, true, deltas);
        else
            delta = new ClusterMembershipDelta(oldMembership.getId(), false, deltas);
        
        send(messageFactory.create(Memberships.CORE_GROUP_ADDRESS, new ClusterMembershipMessagePart(delta)));
    }
    
    @Override
    protected void onInstalled(IClusterMembership newMembership)
    {
        installing = false;
    }
    
    private boolean canInstallMembership(long currentTime)
    {
        if (stopped)
            return false;
        
        if (installing)
            return false;
        
        if (lastTrackTime != 0 && currentTime < lastTrackTime + trackPeriod)
            return false;

        lastTrackTime = currentTime;

        if (flushManager.isFlushInProgress())
            return false;

        IMembership membership = membershipManager.getMembership();
        if (membership == null || !membership.getGroup().getCoordinator().equals(membershipManager.getLocalNode()))
            return false;
        
        if (!failureDetector.getFailedMembers().isEmpty() || !failureDetector.getLeftMembers().isEmpty())
            return false;
        
        for (IClusterMembershipProvider provider : membershipProviders)
        {
            if (provider.hasChanges())
                return true;
        }
        
        return false;
    }
}
