/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;

/**
 * The {@link CoreGroupSubChannel} is a core group node sub-channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class CoreGroupSubChannel extends SubChannel implements IChannel
{
    private final CoreGroupMembershipManager membershipManager;
    
    public CoreGroupSubChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory, 
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider, compartment);
   
        Assert.notNull(membershipManager);
        
        this.membershipManager = membershipManager;
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
}
