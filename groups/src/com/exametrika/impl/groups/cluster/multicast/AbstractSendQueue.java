/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.message.IWrapperMessagePart;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;


/**
 * The {@link AbstractSendQueue} is a send queue of durable failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractSendQueue
{
    private final ISender sender;
    private final ITimeService timeService;
    private final IDeliveryHandler deliveryHandler;
    private final boolean durable;
    private final SimpleDeque<IMessage> deque = new SimpleDeque<IMessage>();
    protected final Map<IAddress, Long> acknowledgedMessageIds = new HashMap<IAddress, Long>();
    private boolean completionRequired;
    private long startMessageId = 1;
    protected long lastCompletedMessageId;
    private long lastCompletionSendTime;
    private long nextSendMessageId = 1;
    private long bundleCreationTime;
    private long lastSendMessageId;
    private long lastOldMembershipMessageId;
    private final QueueCapacityController capacityController;
    private final GroupAddress groupAddress;
    private final UUID groupId;
    private IFlowController<RemoteFlowId> flowController;
    private final IMessageFactory messageFactory;
    private final ILogger logger;
    private final IMarker marker;
    private ICompartment compartment;
    private long lockCount;
    private volatile ArrayList<MulticastSink> sinks = new ArrayList<MulticastSink>();
    private volatile boolean canWrite = true;
    
    public AbstractSendQueue(ISender sender, ITimeService timeService, 
        IDeliveryHandler deliveryHandler, boolean durable, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IMessageFactory messageFactory, GroupAddress groupAddress, UUID groupId,
        ILogger logger, IMarker marker)
    {
        Assert.notNull(sender);
        Assert.notNull(timeService);
        Assert.isTrue(!durable || deliveryHandler != null);
        Assert.notNull(messageFactory);
        Assert.notNull(logger);
        Assert.notNull(marker);
        
        this.sender = sender;
        this.timeService = timeService;
        this.deliveryHandler = deliveryHandler;
        this.messageFactory = messageFactory;
        this.durable = durable;
        this.capacityController = new QueueCapacityController(minLockQueueCapacity, maxUnlockQueueCapacity, 
            groupAddress, groupId);
        this.groupAddress = groupAddress;
        this.groupId = groupId;
        this.logger = logger;
        this.marker = marker;
    }
    
    public void setFlowController(IFlowController<RemoteFlowId> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
        capacityController.setFlowController(flowController);
    }
    
    public void setCompartment(ICompartment compartment)
    {
        Assert.notNull(compartment);
        Assert.isNull(this.compartment);
        
        this.compartment = compartment;
    }
    
    public boolean isCompletionRequired()
    {
        return completionRequired;
    }
    
    public long getLastCompletionSendTime()
    {
        return lastCompletionSendTime;
    }
    
    public long getBundleCreationTime()
    {
        return bundleCreationTime;
    }
    
    public boolean isLastOldMembershipMessageCompleted()
    {
        return lastCompletedMessageId >= lastOldMembershipMessageId;
    }
    
    public void setLastOldMembershipMessageId()
    {
        lastOldMembershipMessageId = lastSendMessageId;
    }
    
    public int getQueueCapacity()
    {
        return capacityController.getCapacity();
    }
    
    public void onMemberFailed(INode member)
    {
        if (acknowledgedMessageIds.remove(member.getAddress()) != null)
            setCompletionRequired();
    }

    public long acquireMessageId()
    {
        return nextSendMessageId++;
    }
    
    public void offer(IMessage message, long messageId)
    {
        Assert.isTrue(messageId == startMessageId + deque.size());
        
        if (lastSendMessageId == startMessageId + deque.size() - 1)
            bundleCreationTime = timeService.getCurrentTime();
        
        deque.offer(message);
        capacityController.addCapacity(message.getSource(), message.getSize());
    }
    
    public List<IMessage> createBundle()
    {
        int pos = (int)(lastSendMessageId - startMessageId + 1);
        if (pos >= deque.size())
            return null;
        
        List<IMessage> bundle = new ArrayList<IMessage>();
        for (int i = pos; i < deque.size(); i++)
        {
            IMessage message = deque.get(i);
            Assert.notNull(bundle);
            
            bundle.add(message);
        }
        
        lastSendMessageId = startMessageId + deque.size() - 1;
        
        if (!durable)
        {
            startMessageId += deque.size();
            deque.clear();
            capacityController.clearCapacity();
        }
        
        return bundle;
    }
    
    public void acknowledge(IAddress address, long lastReceivedMessageId)
    {
        long prevReceivedMessageId = acknowledgedMessageIds.put(address, lastReceivedMessageId);
        Assert.isTrue(prevReceivedMessageId < lastReceivedMessageId);
        
        setCompletionRequired();
    }
    
    public long complete()
    {
        if (!completionRequired)
            return 0;
        
        long minCompletedMessageId = Long.MAX_VALUE;
        for (long value : acknowledgedMessageIds.values())
        {
            if (minCompletedMessageId > value)
                minCompletedMessageId = value;
        }
        
        completionRequired = false;
        
        if (minCompletedMessageId <= lastCompletedMessageId || minCompletedMessageId == Long.MAX_VALUE)
            return 0;
        
        if (durable)
        {
            for (long i = lastCompletedMessageId + 1; i <= minCompletedMessageId; i++)
            {
                IMessage message = deque.poll();
                Assert.checkState(message != null);
                
                deliver(message);
                capacityController.removeCapacity(message.getSize());
            }
            
            startMessageId = minCompletedMessageId + 1;
        }
        
        lastCompletedMessageId = minCompletedMessageId;
        lastCompletionSendTime = timeService.getCurrentTime();
        
        return minCompletedMessageId;
    }

    public void lockFlow(RemoteFlowId flow)
    {
        Assert.isTrue(flow.getFlowId().equals(groupId));
        lockCount++;
        flowController.lockFlow(flow);
    }

    public void unlockFlow(RemoteFlowId flow)
    {
        Assert.isTrue(flow.getFlowId().equals(groupId));
        lockCount--;
        flowController.unlockFlow(flow);
    }
    
    public synchronized ISink register(IFeed feed)
    {
        Assert.notNull(feed);
        
        MulticastSink sink = new MulticastSink(this, groupAddress, feed, messageFactory);
        
        ArrayList<MulticastSink> sinks = (ArrayList<MulticastSink>)this.sinks.clone();
        sinks.add(sink);
        
        this.sinks = sinks;
        
        return sink;
    }
    
    public synchronized void unregister(MulticastSink sink)
    {
        Assert.notNull(sink);
        
        sink.setInvalid();
        
        ArrayList<MulticastSink> sinks = (ArrayList<MulticastSink>)this.sinks.clone();
        sinks.remove(sink);
        
        this.sinks = sinks;
    }
    
    public void updateWriteStatus()
    {
        boolean canWrite = false;
        synchronized (this)
        {
            List<MulticastSink> sinks = this.sinks;
            for (MulticastSink sink : sinks)
            {
                if (sink.canWrite())
                {
                    canWrite = true;
                    break;
                }
            }
            
            this.canWrite = canWrite;
        }
        
        if (canWrite && compartment != null)
            compartment.wakeup();
    }
    
    public boolean send(IMessage message)
    {
        sender.send(message);
        return canWrite();
    }
    
    public void process()
    {
        if (!canWrite())
            return;
        
        List<MulticastSink> sinks = this.sinks;
        for (MulticastSink sink : sinks)
            sink.onWrite();
    }

    public void stop()
    {
        for (MulticastSink sink : sinks)
            sink.setInvalid();
        
        sinks = new ArrayList<MulticastSink>();
    }
    
    protected boolean canWrite()
    {
        return lockCount == 0 && !capacityController.isLocked() && canWrite;
    }
    
    private void setCompletionRequired()
    {
        if (!completionRequired)
        {
            lastCompletionSendTime = timeService.getCurrentTime();
        
            completionRequired = true;
        }
    }
    
    private void deliver(IMessage message)
    {
        message = message.removePart();
        
        if (message.getPart() instanceof TotalOrderMessagePart)
            return;
        else if (message.getPart() instanceof IWrapperMessagePart)
        {
            IWrapperMessagePart part = message.getPart();
            message = part.getMessage();
            Assert.notNull(message);
        }
        
        try
        {
            deliveryHandler.onDelivered(message);
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

