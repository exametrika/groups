/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.ICoreNodeDiscoverer;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link CoreGroupMembershipManager} manages core group membership information.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreGroupMembershipManager extends GroupMembershipManager
{
    private ICoreNodeDiscoverer nodeDiscoverer;
    private IChannelReconnector channelReconnector;
    
    public CoreGroupMembershipManager(String channelName, LocalNodeProvider localNodeProvider,
        Set<IPreparedGroupMembershipListener> preparedMembershipListeners, Set<IGroupMembershipListener> membershipListeners)
    {
        super(channelName, localNodeProvider, preparedMembershipListeners, membershipListeners);
    }
    
    public void setNodeDiscoverer(ICoreNodeDiscoverer nodeDiscoverer)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.isNull(this.nodeDiscoverer);
        
        this.nodeDiscoverer = nodeDiscoverer;
    }
    
    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        Assert.notNull(channelReconnector);
        Assert.isNull(this.channelReconnector);
        
        this.channelReconnector = channelReconnector;
    }
    
    @Override
    public void prepareInstallMembership(IGroupMembership membership)
    {
        super.prepareInstallMembership(membership);
        
        checkMembership(membership);
    }
    
    @Override
    public void prepareChangeMembership(IGroupMembership membership, IGroupMembershipChange membershipChange)
    {
        super.prepareChangeMembership(membership, membershipChange);
        
        checkMembership(membership);
    }
    
    @Override
    public void start()
    {
        super.start();
        Assert.checkState(nodeDiscoverer != null);
        
        nodeDiscoverer.startDiscovery();
    }
    
    private void checkMembership(IGroupMembership membership)
    {
        if (!membership.getGroup().isPrimary())
            channelReconnector.reconnect();
    }
}
