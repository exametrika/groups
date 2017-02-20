/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.channel;

import java.util.List;

import com.exametrika.api.groups.core.IGroupChannel;
import com.exametrika.api.groups.core.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.core.membership.MembershipManager;

/**
 * The {@link GroupChannel} is a group channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupChannel extends Channel implements IGroupChannel, IChannelReconnector
{
    private final MembershipManager membershipManager;
    private final List<IGracefulCloseStrategy> gracefulCloseStrategies;
    private final long gracefulCloseTimeout;
    private final Object condition = new Object();
    private boolean canClose;
    
    public GroupChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver,
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory,
        IConnectionProvider connectionProvider, ICompartment compartment, MembershipManager membershipManager, 
        List<IGracefulCloseStrategy> gracefulCloseStrategies, long gracefulCloseTimeout)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider,
            compartment);
        
        Assert.notNull(membershipManager);
        Assert.notNull(gracefulCloseStrategies);
        
        this.membershipManager = membershipManager;
        this.gracefulCloseStrategies = gracefulCloseStrategies;
        this.gracefulCloseTimeout = gracefulCloseTimeout > 0 ? gracefulCloseTimeout : Integer.MAX_VALUE;
    }

    @Override
    public IMembershipService getMembershipService()
    {
        return membershipManager;
    }

    @Override
    public void close(boolean gracefully)
    {
        if (stopped)
            return;
        
        if (gracefully)
            waitGracefulClose();
        
        stop();
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
            }
        }).start();
    }

    @Override
    protected void doStart()
    {
        membershipManager.start();
    }
    
    @Override
    protected void doStop()
    {
        membershipManager.stop();
    }
    
    private void waitGracefulClose()
    {
        compartment.addTimerProcessor(new ICompartmentTimerProcessor()
        {
            private long lastCheckTime;
            
            @Override
            public void onTimer(long currentTime)
            {
                if (lastCheckTime != 0 && currentTime < lastCheckTime + 500)
                    return;
                
                lastCheckTime = currentTime;
                
                for (IGracefulCloseStrategy strategy : gracefulCloseStrategies)
                {
                    if (!strategy.requestClose())
                        return;
                }
         
                membershipManager.uninstallMembership(LeaveReason.GRACEFUL_CLOSE);
                
                synchronized (condition)
                {
                    canClose = true;
                    condition.notify();
                }
            }
        });
        
        synchronized (condition)
        {
            try
            {
                long startTime = Times.getCurrentTime();
                while (!canClose && Times.getCurrentTime() < startTime + gracefulCloseTimeout)
                    condition.wait(gracefulCloseTimeout);
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);    
            }
        }
    }
}
