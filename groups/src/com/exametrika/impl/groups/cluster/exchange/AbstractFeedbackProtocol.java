/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipManager;

/**
 * The {@link AbstractFeedbackProtocol} represents an abstract feedback data exchange protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractFeedbackProtocol extends AbstractProtocol implements IClusterMembershipListener
{
    private final IClusterMembershipManager membershipManager;
    private final List<IFeedbackProvider> feedbackProviders;
    private final List<IFeedbackListener> listeners;
    private final long dataExchangePeriod;
    private long lastDataExchangeTime;
    private boolean joined;
    
    public AbstractFeedbackProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager, 
        List<IFeedbackProvider> feedbackProviders, List<IFeedbackListener> listeners, long dataExchangePeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(feedbackProviders);
        Assert.notNull(listeners);
        
        this.membershipManager = membershipManager;
        this.feedbackProviders = feedbackProviders;
        this.listeners = listeners;
        this.dataExchangePeriod = dataExchangePeriod;
    }

    @Override
    public void onJoined()
    {
        joined = true;
        updateClusterMembership();
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        updateClusterMembership();
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onTimer(long currentTime)
    {
        if (!joined)
            return;
        
        if (currentTime < lastDataExchangeTime + dataExchangePeriod)
            return;

        lastDataExchangeTime = currentTime;
        
        sendData(false);
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FeedbackMessagePartSerializer());
        
        for (IFeedbackProvider provider : feedbackProviders)
            provider.register(registry);
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FeedbackMessagePartSerializer.ID);
        
        for (IFeedbackProvider provider : feedbackProviders)
            provider.unregister(registry);
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof FeedbackMessagePart)
        {
            FeedbackMessagePart part = message.getPart();
            
            for (IFeedbackProvider provider : feedbackProviders)
            {
                IExchangeData data = part.getDataExchanges().get(provider.getId());
                if (data != null)
                {
                    provider.setData(data);
                    onDataChanged(provider, data);
                }
            }
        }
        else
            receiver.receive(message);
    }
    
    protected void sendData(boolean force)
    {
        IAddress destination = getDestination();
        if (destination == null || membershipManager.getLocalNode().getAddress().equals(destination))
            return;
        
        Map<UUID, IExchangeData> dataExchanges = new LinkedHashMap<UUID, IExchangeData>();
        for (IFeedbackProvider provider : feedbackProviders)
        {
            IExchangeData data = provider.getData(force);
            if (data != null)
                dataExchanges.put(provider.getId(), data);
        }
        
        if (!dataExchanges.isEmpty())
            getFeedbackSender().send(messageFactory.create(destination, new FeedbackMessagePart(dataExchanges)));
    }

    protected abstract IAddress getDestination();
    
    protected ISender getFeedbackSender()
    {
        return getSender();
    }
    
    private void updateClusterMembership()
    {
        IClusterMembership membership = membershipManager.getMembership();
        for (IFeedbackProvider provider : feedbackProviders)
            provider.onClusterMembershipChanged(membership);
    }
    
    private void onDataChanged(IFeedbackProvider provider, IExchangeData data)
    {
        for (IFeedbackListener listener : listeners)
        {
            try
            {
                listener.onDataChanged(provider, data);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
}
