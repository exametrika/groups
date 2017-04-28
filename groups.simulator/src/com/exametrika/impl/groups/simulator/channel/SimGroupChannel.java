/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.channel;

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
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link SimGroupChannel} is a test group channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class SimGroupChannel extends Channel implements IChannelReconnector
{
    private final CoreGroupMembershipManager membershipManager;
    private final IChannelReconnector channelReconnector;
    
    public SimGroupChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory, 
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager, 
        IChannelReconnector channelReconnector)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider, compartment);
   
        Assert.notNull(membershipManager);
        
        this.membershipManager = membershipManager;
        this.channelReconnector = channelReconnector;
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
    }
    
    @Override
    public void stop()
    {
        super.start();
        
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
