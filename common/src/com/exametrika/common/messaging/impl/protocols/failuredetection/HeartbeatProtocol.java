/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;


/**
 * The {@link HeartbeatProtocol} is a failure detection protocol based on heartbeats.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class HeartbeatProtocol extends AbstractProtocol
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final INodeTrackingStrategy nodeTrackingStrategy;
    private final long trackPeriod;
    private final long heartbeatStartPeriod;
    private final long heartbeatPeriod;
    private final long failureDetectionPeriod;
    private IFailureObserver failureObserver;
    private INodeAccessTimeProvider accessTimeProvider;
    private IChannel channel;
    private volatile Map<IAddress, HeartbeatInfo> heartbeats = new LinkedHashMap<IAddress, HeartbeatInfo>();
    private long lastTrackTime;
    private long lastLiveId = -1;

    /**
     * Creates a new object.
     * 
     * @param channelName channel name
     * @param nodeTrackingStrategy node tracking strategy
     * @param messageFactory message factory
     * @param trackPeriod period in milliseconds of node tracking timer
     * @param heartbeatStartPeriod period in milliseconds since last tracked node response after which protocol begin to send heartbeats
     *        to that node
     * @param heartbeatPeriod period in milliseconds between subsequent heartbeats
     * @param failureDetectionPeriod period of failure detection in milliseconds
     */
    public HeartbeatProtocol(String channelName, INodeTrackingStrategy nodeTrackingStrategy,
        IMessageFactory messageFactory, long trackPeriod, long heartbeatStartPeriod, long heartbeatPeriod, long failureDetectionPeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(nodeTrackingStrategy);
        Assert.isTrue (failureDetectionPeriod >= heartbeatStartPeriod);

        this.nodeTrackingStrategy = nodeTrackingStrategy;
        this.trackPeriod = trackPeriod;
        this.heartbeatStartPeriod = heartbeatStartPeriod;
        this.heartbeatPeriod = heartbeatPeriod;
        this.failureDetectionPeriod = failureDetectionPeriod;
    }

    public void setFailureObserver(IFailureObserver failureObserver)
    {
        Assert.notNull(failureObserver);
        Assert.isNull(this.failureObserver);
        this.failureObserver = failureObserver;
    }
    
    public void setAccessTimeProvider(INodeAccessTimeProvider accessTimeProvider)
    {
        Assert.notNull(accessTimeProvider);
        Assert.isNull(this.accessTimeProvider);
        this.accessTimeProvider = accessTimeProvider;
    }
    
    public void setChannel(IChannel channel)
    {
        Assert.notNull(channel);
        Assert.isNull(this.channel);
        
        this.channel = channel;
    }
    
    public INodeTrackingStrategy getNodeTrackingStrategy()
    {
        return nodeTrackingStrategy;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (lastTrackTime != 0 && currentTime - lastTrackTime < trackPeriod)
            return;
        
        lastTrackTime = currentTime;

        trackNodes();
    }
    
    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        if (lastLiveId != liveNodeProvider.getId())
        {
            lastLiveId = liveNodeProvider.getId();
            
            Set<IAddress> trackedNodes = nodeTrackingStrategy.getTrackedNodes(liveNodeProvider.getLocalNode(), 
                liveNodeProvider.getLiveNodes());
                    
            Map<IAddress, HeartbeatInfo> heartbeats = new LinkedHashMap<IAddress, HeartbeatInfo>();
            for (IAddress node : trackedNodes)
            {
                HeartbeatInfo info = this.heartbeats.get(node);
                if (info == null)
                {
                    info = new HeartbeatInfo();
                    info.lastRequestTime = currentTime;
                }
                    
                heartbeats.put(node, info);
            }
            
            this.heartbeats = heartbeats;
        }
    }
    
    @Override
    public void start()
    {
        super.start();
        Assert.checkState(failureObserver != null);
        Assert.checkState(accessTimeProvider != null);
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.hasFlags(MessageFlags.HEARTBEAT_REQUEST))
        {
            IMessage response = messageFactory.create(message.getSource(), MessageFlags.HEARTBEAT_RESPONSE | 
                MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL);
            if (channel != null)
                channel.send(response);
            else
                send(response);
        }
        else if (!message.hasFlags(MessageFlags.HEARTBEAT_RESPONSE))
            receiver.receive(message);
    }

    private void trackNodes()
    {
        long currentTime = timeService.getCurrentTime();
        
        Map<IAddress, HeartbeatInfo> heartbeats = this.heartbeats;

        for (Map.Entry<IAddress, HeartbeatInfo> entry : heartbeats.entrySet())
        {
            IAddress node = entry.getKey();
            HeartbeatInfo info = entry.getValue();
            long lastResponseTime = accessTimeProvider.getLastReadTime(node);
            if (lastResponseTime == 0)
                continue;
            
            if (currentTime - lastResponseTime >= failureDetectionPeriod)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.failedNode(node));
                    
                failureObserver.onNodesFailed(Collections.singleton(node));
            }
            else if (currentTime - lastResponseTime >= heartbeatStartPeriod && currentTime - info.lastRequestTime >= heartbeatPeriod)
            {
                info.lastRequestTime = currentTime;
                send(messageFactory.create(node, MessageFlags.HEARTBEAT_REQUEST | MessageFlags.HIGH_PRIORITY |
                    MessageFlags.PARALLEL));
            }
        }
    }

    private static final class HeartbeatInfo
    {
        private long lastRequestTime;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Failed node ''{0}'' has been detected.")
        ILocalizedMessage failedNode(IAddress node);
    }
}
