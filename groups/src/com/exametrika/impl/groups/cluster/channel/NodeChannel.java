/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.INodeChannel;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.CompositeChannel;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link NodeChannel} is a node node channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class NodeChannel extends CompositeChannel implements INodeChannel, IChannelReconnector
{
    protected final ClusterMembershipManager membershipManager;
    private final List<IGracefulExitStrategy> gracefulExitStrategies;
    private final long gracefulExitTimeout;
    private final  IChannelReconnector channelReconnector;
    private final Object condition = new Object();
    private boolean canClose;
    private boolean startWait;
    
    public NodeChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        List<IChannel> subChannels, IChannel mainSubChannel, ICompartment compartment, ClusterMembershipManager membershipManager, 
        List<IGracefulExitStrategy> gracefulExitStrategies, long gracefulExitTimeout, IChannelReconnector channelReconnector)
    {
        super(channelName, liveNodeManager, channelObserver, subChannels, mainSubChannel, compartment);;
        
        Assert.notNull(membershipManager);
        Assert.notNull(gracefulExitStrategies);
        
        this.membershipManager = membershipManager;
        this.gracefulExitStrategies = gracefulExitStrategies;
        this.gracefulExitTimeout = gracefulExitTimeout > 0 ? gracefulExitTimeout : Integer.MAX_VALUE;
        this.channelReconnector = channelReconnector;
    }

    @Override
    public IClusterMembershipService getMembershipService()
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
                if (channelReconnector != null)
                    channelReconnector.reconnect();
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
    
    protected void doStartWaitGracefulExit()
    {
    }
    
    private void waitGracefulExit()
    {
        boolean waitStarted;
        synchronized (this)
        {
            waitStarted = this.startWait;
            this.startWait = true;
        }
        
        if (!waitStarted)
        {
            doStartWaitGracefulExit();
            
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
        }
        
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
