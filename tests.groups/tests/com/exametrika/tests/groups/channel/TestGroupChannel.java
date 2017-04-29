/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link TestGroupChannel} is a test group channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestGroupChannel extends Channel implements IChannelReconnector
{
    private final CoreGroupMembershipManager membershipManager;
    private final IChannelReconnector channelReconnector;
    private final CoreGroupMembershipTracker groupMembershipTracker;
    
    public TestGroupChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory, 
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager, 
        IChannelReconnector channelReconnector, CoreGroupMembershipTracker groupMembershipTracker)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider, compartment);
   
        Assert.notNull(membershipManager);
        Assert.notNull(groupMembershipTracker);
        
        this.membershipManager = membershipManager;
        this.channelReconnector = channelReconnector;
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
    
    @Override
    public void reconnect()
    {
        membershipManager.uninstallMembership(LeaveReason.RECONNECT);
        
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                stop();
                if (channelReconnector != null)
                    channelReconnector.reconnect();
            }
        }).start();
    }
}
