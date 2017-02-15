/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;

/**
 * The {@link FlowControlProtocol} represents a flow control protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FlowControlProtocol extends AbstractProtocol implements IFailureDetectionListener, 
    IFlowController<RemoteFlowId>
{
    private final IFlowController<RemoteFlowId> flowController;
    private IFailureDetector failureDetector;
    private final IMembershipService membershipService;
    private final Set<Pair<IAddress, UUID>> remoteLocks = new LinkedHashSet<Pair<IAddress, UUID>>();
    private final Map<Pair<IAddress, UUID>, LockFlowInfo> localLocks = new HashMap<Pair<IAddress, UUID>, LockFlowInfo>();
    
    public FlowControlProtocol(String channelName, IMessageFactory messageFactory, IFlowController<RemoteFlowId> flowController,
        IMembershipService membershipService)
    {
        super(channelName, messageFactory);
       
        Assert.notNull(flowController);
        Assert.notNull(membershipService);
        
        this.flowController = flowController;
        this.membershipService = membershipService;
    }

    public void setFailureDetector(IFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        for (Iterator<Pair<IAddress, UUID>> it = remoteLocks.iterator(); it.hasNext();)
        {
            Pair<IAddress, UUID> pair = it.next();
            if (pair.getKey().equals(member.getAddress()))
            {
                RemoteFlowId flowId = new RemoteFlowId(membershipService.getLocalNode().getAddress(), pair.getKey(),
                    pair.getValue());
                flowController.unlockFlow(flowId);
                
                it.remove();
            }
        }
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }

    @Override
    public void lockFlow(RemoteFlowId flow)
    {
        Assert.notNull(flow);
        
        Pair<IAddress, UUID> key = new Pair<IAddress, UUID>(flow.getSender(), flow.getFlowId());
        LockFlowInfo info = localLocks.get(key);
        if (info == null)
        {
            info = new LockFlowInfo();
            localLocks.put(key, info);
           
            if (failureDetector.isHealthyMember(flow.getSender().getId()))
                send(messageFactory.create(flow.getSender(), new FlowControlMessagePart(flow.getFlowId(), true)));
        }
        
        info.lockCount++;
    }

    @Override
    public void unlockFlow(RemoteFlowId flow)
    {
        Assert.notNull(flow);
        
        Pair<IAddress, UUID> key = new Pair<IAddress, UUID>(flow.getSender(), flow.getFlowId());
        LockFlowInfo info = localLocks.get(key);
        Assert.notNull(info);
            
        info.lockCount--;
        if (info.lockCount == 0)
        {
            localLocks.remove(key);
            
            if (failureDetector.isHealthyMember(flow.getSender().getId()))
                send(messageFactory.create(flow.getSender(), new FlowControlMessagePart(flow.getFlowId(), false)));
        }
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FlowControlMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FlowControlMessagePartSerializer.ID);
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof FlowControlMessagePart)
        {
            FlowControlMessagePart part = message.getPart();
            Pair<IAddress, UUID> key = new Pair<IAddress, UUID>(message.getSource(), part.getFlowId());
            RemoteFlowId flowId = new RemoteFlowId(message.getDestination(), message.getSource(), part.getFlowId());
            
            if (part.isBlocked())
            {
                Assert.checkState(remoteLocks.add(key));

                flowController.lockFlow(flowId);
            }
            else
            {
                Assert.checkState(remoteLocks.remove(key));
                
                flowController.unlockFlow(flowId);
            }
        }
        else
            receiver.receive(message);
    }

    private static class LockFlowInfo
    {
        private long lockCount;
    }
}
