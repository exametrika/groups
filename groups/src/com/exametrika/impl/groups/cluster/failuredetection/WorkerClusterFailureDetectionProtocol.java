/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.channel.IChannelReconnector;
import com.exametrika.impl.groups.cluster.membership.IWorkerControllerObserver;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipChange;

/**
 * The {@link WorkerClusterFailureDetectionProtocol} represents a worker node part of cluster failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerClusterFailureDetectionProtocol extends AbstractProtocol implements IClusterMembershipListener,
    IFailureObserver, IWorkerControllerObserver
{
    private final IClusterMembershipService membershipService;
    private IChannelReconnector channelReconnector;
    private final long failureHistoryPeriod;
    private final int maxShunCount;
    private final long nodeOrphanPeriod;
    private Map<IAddress, FailureInfo> failureHistory = new LinkedHashMap<IAddress, FailureInfo>();
    private IAddress controller;
    private long controllerStartFailureTime;

    public WorkerClusterFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipService membershipService, long failureHistoryPeriod, int maxShunCount,
        long nodeOrphanPeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        
        this.membershipService = membershipService;
        this.failureHistoryPeriod = failureHistoryPeriod;
        this.maxShunCount = maxShunCount;
        this.nodeOrphanPeriod = nodeOrphanPeriod;
    }

    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        Assert.notNull(channelReconnector);
        Assert.isNull(this.channelReconnector);
        
        this.channelReconnector = channelReconnector;
    }
    
    public IAddress getController()
    {
        return controller;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (controllerStartFailureTime != 0 && currentTime > controllerStartFailureTime + nodeOrphanPeriod)
            channelReconnector.reconnect();
        
        if (!failureHistory.isEmpty())
        {
            for (Iterator<FailureInfo> it = failureHistory.values().iterator(); it.hasNext(); )
            {
                FailureInfo info = it.next();
                if (currentTime > info.time + failureHistoryPeriod)
                    it.remove();
                else
                    break;
            }
        }
    }

    @Override
    public void onJoined()
    {
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        INode localNode = membershipService.getLocalNode();
        IDomainMembershipChange domain = event.getMembershipChange().findChangedDomain(localNode.getDomain());
        if (domain == null)
            return;
        
        long currentTime = timeService.getCurrentTime();
        NodesMembershipChange change = domain.findChange(NodesMembershipChange.class);
        
        for (INode node : change.getFailedNodes())
            failureHistory.put(node.getAddress(), new FailureInfo(currentTime, true));
        for (INode node : change.getLeftNodes())
            failureHistory.put(node.getAddress(), new FailureInfo(currentTime, false));
    }
    
    @Override
    public void onControllerChanged(IAddress controller)
    {
        Assert.notNull(controller);
        
        this.controller = controller;
        controllerStartFailureTime = 0;
    }

    @Override
    public void onNodesFailed(Set<IAddress> nodes)
    {
        if (nodes.contains(controller))
            controllerStartFailureTime = timeService.getCurrentTime();
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        if (nodes.contains(controller))
            controllerStartFailureTime = timeService.getCurrentTime();
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        FailureInfo info = failureHistory.get(message.getSource());
        if (info != null)
        {
            if (info.failed && info.shunCount < maxShunCount)
            {
                send(messageFactory.create(message.getSource(), MessageFlags.SHUN | MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                info.shunCount++;
            }
            
            return;
        }
        else if (message.hasFlags(MessageFlags.SHUN))
            channelReconnector.reconnect();
        else
            receiver.receive(message);
    }

    private static class FailureInfo
    {
        private final long time;
        private final boolean failed;
        private int shunCount;
        
        public FailureInfo(long time, boolean failed)
        {
            this.time = time;
            this.failed = failed;
        }
    }
}
