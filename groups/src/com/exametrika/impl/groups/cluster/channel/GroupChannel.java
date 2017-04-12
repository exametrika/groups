/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroupChannel;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
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
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;

/**
 * The {@link GroupChannel} is a group channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupChannel extends Channel implements IGroupChannel, IChannelReconnector
{
    private final CoreGroupMembershipManager membershipManager;
    private final List<IGracefulExitStrategy> gracefulExitStrategies;
    private final long gracefulExitTimeout;
    private final Object condition = new Object();
    private boolean canClose;
    
    public GroupChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver,
        ProtocolStack protocolStack, ITransport transport, IMessageFactory messageFactory,
        IConnectionProvider connectionProvider, ICompartment compartment, CoreGroupMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout)
    {
        super(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, connectionProvider,
            compartment);
        
        Assert.notNull(membershipManager);
        Assert.notNull(gracefulExitStrategies);
        
        this.membershipManager = membershipManager;
        this.gracefulExitStrategies = gracefulExitStrategies;
        this.gracefulExitTimeout = gracefulExitTimeout > 0 ? gracefulExitTimeout : Integer.MAX_VALUE;
    }

    @Override
    public IGroupMembershipService getMembershipService()
    {
        return membershipManager;
    }

    @Override
    public void close(boolean gracefully)
    {
        if (stopped)
            return;
        
        if (gracefully)
            waitGracefulExit();
        
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
    
    private void waitGracefulExit()
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
                
                for (IGracefulExitStrategy strategy : gracefulExitStrategies)
                {
                    if (!strategy.requestExit())
                        return;
                }
         
                membershipManager.uninstallMembership(LeaveReason.GRACEFUL_EXIT);
                
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
                while (!canClose && Times.getCurrentTime() < startTime + gracefulExitTimeout)
                    condition.wait(gracefulExitTimeout);
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);    
            }
        }
    }
}
