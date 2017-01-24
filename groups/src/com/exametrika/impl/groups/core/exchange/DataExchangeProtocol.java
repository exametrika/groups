/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.INode;
import com.exametrika.api.groups.core.MembershipEvent;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.membership.IMembershipManager;

/**
 * The {@link DataExchangeProtocol} represents a data exchange protocol based on token circulating in group.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DataExchangeProtocol extends AbstractProtocol implements IMembershipListener, IFailureDetectionListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;
    private final long minDataExchangePeriod;
    private final long maxDataExchangePeriod;
    private final List<IDataExchangeProvider> dataExchangeProviders;
    private final Random random = new Random();
    private final Map<UUID, ProviderExchangeInfo> providerExchanges = new HashMap<UUID, ProviderExchangeInfo>();
    private INode coordinator;
    private List<INode> healthyMembers;
    private long nextDataExchangeTime;
    private boolean fullExchange;

    public DataExchangeProtocol(String channelName, IMessageFactory messageFactory, IMembershipManager membershipManager, 
        IFailureDetector failureDetector, List<IDataExchangeProvider> dataExchangeProviders,
        long minDataExchangePeriod, long maxDataExchangePeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(dataExchangeProviders);
        Assert.isTrue(minDataExchangePeriod >= 0 && minDataExchangePeriod <= maxDataExchangePeriod 
            && maxDataExchangePeriod <= Integer.MAX_VALUE);
        
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.minDataExchangePeriod = minDataExchangePeriod;
        this.maxDataExchangePeriod = maxDataExchangePeriod;
        this.dataExchangeProviders = dataExchangeProviders;
    }

    @Override
    public void start()
    {
        super.start();
        
        IAddress localAddress = membershipManager.getLocalNode().getAddress();
        for (IDataExchangeProvider provider : dataExchangeProviders)
            Assert.isNull(providerExchanges.put(provider.getId(), new ProviderExchangeInfo(provider, localAddress)));
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        updateCoordinator();
    }

    @Override
    public void onMemberLeft(INode member)
    {
        updateCoordinator();
    }

    @Override
    public void onJoined()
    {
        updateCoordinator();
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(MembershipEvent event)
    {
        updateCoordinator();
    }

    @Override
    public void onTimer(long currentTime)
    {
        if (!fullExchange && currentTime < nextDataExchangeTime)
            return;

        Map<UUID, ProviderExchangeData> exchangeData = null;
        for (Map.Entry<UUID, ProviderExchangeInfo> providerEntry : providerExchanges.entrySet())
        {
            ProviderExchangeInfo info = providerEntry.getValue();
            IExchangeData data = info.provider.getData();
            if (data != null && info.localExchangeInfo.data != null && data.getId() == info.localExchangeInfo.data.getId())
                continue;
            
            if (data != null)
            {
                info.localExchangeInfo.data = data;
                
                if (coordinator != null)
                {
                    if (exchangeData == null)
                        exchangeData = new HashMap<UUID, ProviderExchangeData>();
                    
                    ProviderExchangeData providerExchangeData = new ProviderExchangeData(Collections.singletonMap(
                        info.localExchangeInfo.address.getId(), data));
                    
                    exchangeData.put(providerEntry.getKey(), providerExchangeData);
                }
                else
                    info.localExchangeInfo.modified = true;
            }
        }
        
        if (exchangeData != null)
            send(messageFactory.create(coordinator.getAddress(), new DataExchangeMessagePart(exchangeData), MessageFlags.HIGH_PRIORITY));
        
        if (healthyMembers)
        {
            for (INode member : healthyMembers)
            {
                
            }
        }
        for (Map.Entry<UUID, ProviderExchangeInfo> providerEntry : providerExchanges.entrySet())
        {
            ProviderExchangeInfo providerInfo = providerEntry.getValue();
            if (!providerInfo.localExchangeLocked)
            {
                IExchangeData data = providerInfo.provider.getData();
                if (data != null)
                {
                    providerInfo.localExchangeLocked = true;
                    providerInfo.localExchangeInfo.id++;
                    providerInfo.localExchangeInfo.data = data;
                    providerInfo.localExchangeInfo.modified = true;
                    providerInfo.modified = true;
                    providerInfo.localSendTime = currentTime;
                }
            }
            else if (currentTime > providerInfo.localSendTime + maxDataExchangePeriod * 
                membershipManager.getMembership().getGroup().getMembers().size())
            {
                providerInfo.localExchangeInfo.modified = true;
                providerInfo.modified = true;
                providerInfo.localSendTime = currentTime;
            }
            
            if (!fullExchange && !providerInfo.modified)
                continue;

            if (groupData == null)
                groupData = new HashMap<UUID, ProviderExchangeData>();
            
            Map<UUID, NodeExchangeData> nodeExchanges = new HashMap<UUID, NodeExchangeData>();
            ProviderExchangeData providerData = new ProviderExchangeData(nodeExchanges);
            groupData.put(providerEntry.getKey(), providerData);
            
            if (fullExchange || providerInfo.localExchangeInfo.modified)
            {
                nodeExchanges.put(membershipManager.getLocalNode().getId(), new NodeExchangeData(providerInfo.localExchangeInfo.id, 
                    providerInfo.localExchangeInfo.data));
                providerInfo.localExchangeInfo.modified = false;
            }
            
            for (Map.Entry<UUID, NodeExchangeInfo> nodeEntry : providerInfo.nodeExchanges.entrySet())
            {
                NodeExchangeInfo nodeInfo = nodeEntry.getValue();
                if (fullExchange || nodeInfo.modified)
                {
                    nodeExchanges.put(nodeEntry.getKey(), new NodeExchangeData(nodeInfo.id, nodeInfo.data));
                    nodeInfo.modified = false;
                }
            }
            
            providerInfo.modified = false;
        }
        
        fullExchange = false;
        
        if (groupData != null)
            send(messageFactory.create(nextNode.getAddress(), new DataExchangeMessagePart(groupData), MessageFlags.HIGH_PRIORITY));
        
        nextDataExchangeTime = timeService.getCurrentTime() + minDataExchangePeriod + 
            random.nextInt((int)(maxDataExchangePeriod - minDataExchangePeriod));
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new DataExchangeMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(DataExchangeMessagePartSerializer.ID);
    }

    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        for (Map.Entry<UUID, ProviderExchangeInfo> entry : providerExchanges.entrySet())
        {
            for (Iterator<Map.Entry<UUID, NodeExchangeInfo>> it = entry.getValue().nodeExchanges.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry<UUID, NodeExchangeInfo> nodeEntry = it.next();
                if (cleanupManager.canCleanup(nodeEntry.getValue().address))
                    it.remove();
            }
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof DataExchangeMessagePart)
        {
            DataExchangeMessagePart part = message.getPart();
            
            IMembership membership = membershipManager.getMembership();
            if (membership == null)
                return;
            
            for (Map.Entry<UUID, ProviderExchangeData> providerEntry : part.getProviderExchanges().entrySet())
            {
                ProviderExchangeData providerData = providerEntry.getValue();
                ProviderExchangeInfo providerInfo = providerExchanges.get(providerEntry.getKey());
                if (providerInfo == null)
                    continue;
                
                for (Map.Entry<UUID, IExchangeData> nodeEntry : providerData.getNodeExchanges().entrySet())
                {
                    IExchangeData nodeData = nodeEntry.getValue();
                    NodeExchangeInfo nodeInfo = providerInfo.nodeExchanges.get(nodeEntry.getKey());
                    if (nodeInfo == null)
                    {
                        nodeInfo = new NodeExchangeInfo(nodeData);
                        providerInfo.nodeExchanges.put(nodeEntry.getKey(), nodeInfo);
                        providerInfo.provider.setData(membershipManager.getPreparedMembership().getGroup().findMember(nodeEntry.getKey()), 
                            nodeData);
                    }
                    else if (nodeData.getId() > nodeInfo.data.getId())
                    {
                        nodeInfo.data = nodeData;
                        nodeInfo.modified = true;
                        providerInfo.provider.setData(membershipManager.getPreparedMembership().getGroup().findMember(nodeEntry.getKey()), 
                            nodeData);
                    }
                }
            }
        }
        else
            receiver.receive(message);
    }

    private void updateCoordinator()
    {
        IMembership membership = membershipManager.getMembership();
        if (membership == null)
            return;

        if (coordinator != null && coordinator.equals(failureDetector.getCurrentCoordinator()))
            return;

        if (!membershipManager.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
        {
            coordinator = failureDetector.getCurrentCoordinator();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.coordinatorChanged(coordinator));
        }
        else
        {
            coordinator = null;
            
            List<INode> healthyMembers = new ArrayList<INode>();
            for (INode node : membership.getGroup().getMembers())
            {
                if (!failureDetector.getFailedMembers().contains(node) && !failureDetector.getLeftMembers().contains(node))
                    healthyMembers.add(node);
            }
            
            this.healthyMembers = healthyMembers;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.healthyMembersChanged(healthyMembers));
        }
        
        fullExchange = true;
        nextDataExchangeTime = timeService.getCurrentTime() + minDataExchangePeriod + 
            random.nextInt((int)(maxDataExchangePeriod - minDataExchangePeriod));
    }

    private static class NodeExchangeInfo
    {
        private final IAddress address;
        private IExchangeData data;
        private boolean modified = true;
        
        public NodeExchangeInfo(IAddress address)
        {
            Assert.notNull(address);
            this.address = address;
        }
    }
    
    private static class ProviderExchangeInfo
    {
        private final IDataExchangeProvider provider;
        private final NodeExchangeInfo localExchangeInfo;
        private final Map<UUID, NodeExchangeInfo> nodeExchanges = new HashMap<UUID, NodeExchangeInfo>();
        
        public ProviderExchangeInfo(IDataExchangeProvider provider, IAddress address)
        {
            Assert.notNull(provider);
            
            this.provider = provider;
            this.localExchangeInfo = new NodeExchangeInfo(address);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Healthy members have changed to ''{0}''.")
        ILocalizedMessage healthyMembersChanged(List<INode> nodes);  
        @DefaultMessage("Coordinator has changed to ''{0}''.")
        ILocalizedMessage coordinatorChanged(INode node);  
    }
}
