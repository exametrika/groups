/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.cluster.discovery.IDiscoveryStrategy;

/**
 * The {@link WorkerClusterDiscoveryProtocol} represents a worker node part of cluster discovery protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerClusterDiscoveryProtocol extends AbstractProtocol implements IClusterMembershipListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IDiscoveryStrategy discoveryStrategy;
    private final ILiveNodeProvider liveNodeProvider;
    private final IClusterMembershipService membershipService;
    private final long discoveryPeriod;
    private final long connectionTimeout;
    private final Random random = new Random();
    private long lastDiscoveryTime;
    private boolean joined;
    private List<String> entryPoints;
    private String entryPoint;
    private IAddress entryPointAddress;
    private long startConnectionTime;

    public WorkerClusterDiscoveryProtocol(String channelName, IMessageFactory messageFactory, IDiscoveryStrategy discoveryStrategy, 
        ILiveNodeProvider liveNodeProvider, IClusterMembershipService membershipService, long discoveryPeriod, long connectionTimeout)
    {
        super(channelName, messageFactory);

        Assert.notNull(discoveryStrategy);
        Assert.notNull(liveNodeProvider);
        Assert.notNull(membershipService);

        this.discoveryStrategy = discoveryStrategy;
        this.liveNodeProvider = liveNodeProvider;
        this.membershipService = membershipService;
        this.discoveryPeriod = discoveryPeriod;
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public void onJoined()
    {
        joined = true;
        entryPoint = null;
        entryPointAddress = null;
        entryPoints = null;
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (joined)
            return;
        
        if (currentTime > lastDiscoveryTime + discoveryPeriod)
        {
            lastDiscoveryTime = currentTime;
            
            IAddress entryPointAddress = selectEntryPoint(currentTime);
            if (entryPointAddress == null)
                return;
            
            DiscoveryMessagePart discoveryPart = new DiscoveryMessagePart(
                java.util.Collections.singleton(membershipService.getLocalNode()), false);
            send(messageFactory.create(entryPointAddress, discoveryPart, MessageFlags.HIGH_PRIORITY));
        }
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new DiscoveryMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(DiscoveryMessagePartSerializer.ID);
    }
    
    private IAddress selectEntryPoint(long currentTime)
    {
        if (entryPoints == null || entryPoints.isEmpty())
        {
            entryPoints = new ArrayList<String>(discoveryStrategy.getEntryPoints());
            if (entryPoints.isEmpty())
                return null;
        }
        
        if (entryPointAddress == null || !liveNodeProvider.isLive(entryPointAddress))
        {
            if (entryPoint == null || currentTime > startConnectionTime + connectionTimeout)
            {
                startConnectionTime = currentTime;
                int index = random.nextInt(entryPoints.size());
                entryPoint = entryPoints.remove(index);
                entryPoint = connectionProvider.canonicalize(entryPoint);
                connectionProvider.connect(entryPoint);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.entryPointSelected(entryPoint));
            }
            
            entryPointAddress = liveNodeProvider.findByConnection(entryPoint);
        }
        
        return entryPointAddress;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Entry point ''{0}'' has been selected.")
        ILocalizedMessage entryPointSelected(String entryPoint);
    }
}
