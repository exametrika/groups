/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;

/**
 * The {@link CoreGroupSubChannel} is a core group node sub-channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreGroupSubChannel extends SubChannel
{
    private final CoreGroupMembershipManager membershipManager;
    private final CoreGroupMembershipTracker groupMembershipTracker;
    
    public CoreGroupSubChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory, 
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager,
        CoreGroupMembershipTracker groupMembershipTracker)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider, compartment);
   
        Assert.notNull(membershipManager);
        Assert.notNull(groupMembershipTracker);
        
        this.membershipManager = membershipManager;
        this.groupMembershipTracker = groupMembershipTracker;
    }
    
    public IGroupMembershipService getMembershipService()
    {
        return membershipManager;
    }
    
    @Override
    public void start()
    {
        super.start();
        
        membershipManager.start();
        groupMembershipTracker.start();
    }
    
    @Override
    public void stop()
    {
        groupMembershipTracker.stop();
        
        super.stop();
        
        membershipManager.stop();
    }
}
