/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final long dataExchangePeriod;
    private final Map<UUID, ProviderExchangeInfo> providerExchanges = new HashMap<UUID, ProviderExchangeInfo>();
    private INode coordinator;
    private List<INode> healthyMembers;
    private long lastDataExchangeTime;
    private boolean fullExchange;

    public DataExchangeProtocol(String channelName, IMessageFactory messageFactory, IMembershipManager membershipManager, 
        IFailureDetector failureDetector, List<IDataExchangeProvider> dataExchangeProviders, long dataExchangePeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(dataExchangeProviders);
        
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.dataExchangePeriod = dataExchangePeriod;
        
        for (IDataExchangeProvider provider : dataExchangeProviders)
            Assert.isNull(providerExchanges.put(provider.getId(), new ProviderExchangeInfo(provider)));
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
        if (coordinator == null && healthyMembers == null)
            return;
        
        if (!fullExchange && currentTime < lastDataExchangeTime + dataExchangePeriod)
            return;

        for (Map.Entry<UUID, ProviderExchangeInfo> providerEntry : providerExchanges.entrySet())
        {
            ProviderExchangeInfo providerInfo = providerEntry.getValue();
            IExchangeData data = providerInfo.provider.getData();
            if (data == null)
                continue;
            
            if (providerInfo.localExchangeInfo.data != null && data.getId() <= providerInfo.localExchangeInfo.data.getId())
                continue;
            
            providerInfo.localExchangeInfo.data = data;
            providerInfo.localExchangeInfo.modified = true;
        }
        
        if (coordinator != null)
        {
            Map<UUID, ProviderExchangeData> exchangeData = null;
            for (Map.Entry<UUID, ProviderExchangeInfo> providerEntry : providerExchanges.entrySet())
            {
                ProviderExchangeInfo providerInfo = providerEntry.getValue();
                if (!fullExchange && !providerInfo.localExchangeInfo.modified)
                    continue;
                
                if (exchangeData == null)
                    exchangeData = new HashMap<UUID, ProviderExchangeData>();
                
                ProviderExchangeData providerExchangeData = new ProviderExchangeData(Collections.singletonMap(
                    membershipManager.getLocalNode().getId(), providerInfo.localExchangeInfo.data));
                
                exchangeData.put(providerEntry.getKey(), providerExchangeData);
                providerInfo.localExchangeInfo.modified = false;
            }
            
            if (exchangeData != null)
                send(messageFactory.create(coordinator.getAddress(), new DataExchangeMessagePart(exchangeData), MessageFlags.HIGH_PRIORITY));
        }
        else
        {
            Assert.checkState(healthyMembers != null);
            
            for (INode member : healthyMembers)
            {
                if (member.equals(membershipManager.getLocalNode()))
                    continue;
                
                Map<UUID, ProviderExchangeData> exchangeData = null;
                for (Map.Entry<UUID, ProviderExchangeInfo> providerEntry : providerExchanges.entrySet())
                {
                    ProviderExchangeInfo providerInfo = providerEntry.getValue();
                    Map<UUID, IExchangeData> nodeExchanges = null;
                    for (Map.Entry<UUID, NodeExchangeInfo> nodeEntry : providerInfo.nodeExchanges.entrySet())
                    {
                        if (nodeEntry.getKey().equals(member.getId()))
                            continue;
                        
                        NodeExchangeInfo nodeInfo = nodeEntry.getValue();
                        nodeExchanges = addExchangeInfo(nodeExchanges, nodeEntry.getKey(), nodeInfo);
                    }
                    
                    nodeExchanges = addExchangeInfo(nodeExchanges, membershipManager.getLocalNode().getId(), providerInfo.localExchangeInfo);
                    
                    if (nodeExchanges != null)
                    {
                        if (exchangeData == null)
                            exchangeData = new HashMap<UUID, ProviderExchangeData>();
                        
                        exchangeData.put(providerEntry.getKey(), new ProviderExchangeData(nodeExchanges));
                    }
                } 
                
                if (exchangeData != null)
                    send(messageFactory.create(member.getAddress(), new DataExchangeMessagePart(exchangeData), MessageFlags.HIGH_PRIORITY));
            }
        }
        
        fullExchange = false;
        lastDataExchangeTime = currentTime;
    }

    private Map<UUID, IExchangeData> addExchangeInfo(Map<UUID, IExchangeData> nodeExchanges, UUID nodeId, NodeExchangeInfo nodeInfo)
    {
        if (!fullExchange && !nodeInfo.modified)
            return nodeExchanges;
        
        if (nodeExchanges == null)
            nodeExchanges = new HashMap<UUID, IExchangeData>();
        
        nodeExchanges.put(nodeId, nodeInfo.data);
        nodeInfo.modified = false;
        return nodeExchanges;
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
                    INode member = membership.getGroup().findMember(nodeEntry.getKey());
                    if (member == null)
                        continue;

                    boolean update = false;
                    NodeExchangeInfo nodeInfo = providerInfo.nodeExchanges.get(nodeEntry.getKey());
                    if (nodeInfo == null)
                    {
                        nodeInfo = new NodeExchangeInfo();
                        nodeInfo.address = member.getAddress();
                        providerInfo.nodeExchanges.put(nodeEntry.getKey(), nodeInfo);
                        update = true;
                    }

                    if (update || nodeData.getId() > nodeInfo.data.getId())
                    {
                        nodeInfo.data = nodeData;
                        nodeInfo.modified = true;
                        providerInfo.provider.setData(member, nodeData);
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
        lastDataExchangeTime = timeService.getCurrentTime();
    }

    private static class NodeExchangeInfo
    {
        private IAddress address;
        private IExchangeData data;
        private boolean modified;
    }
    
    private static class ProviderExchangeInfo
    {
        private final IDataExchangeProvider provider;
        private final NodeExchangeInfo localExchangeInfo = new NodeExchangeInfo();
        private final Map<UUID, NodeExchangeInfo> nodeExchanges = new HashMap<UUID, NodeExchangeInfo>();
        
        public ProviderExchangeInfo(IDataExchangeProvider provider)
        {
            Assert.notNull(provider);
            
            this.provider = provider;
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
