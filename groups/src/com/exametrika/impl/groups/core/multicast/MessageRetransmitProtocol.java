/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.exchange.IExchangeData;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IMembershipManager;


/**
 * The {@link MessageRetransmitProtocol} is a protocol used to retransmit lost messages.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class MessageRetransmitProtocol
{
    private final IFlushParticipant flushParticipant;
    private final IMembershipManager membershipManager;
    private final IMessageFactory messageFactory;
    private final IReceiver receiver;
    private final ISender sender;
    private final ITimeService timeService;
    private final boolean durable;
    private final boolean ordered;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private IFlowController<IAddress> flowController;
    private final Map<IAddress, ReceiveQueue> receiveQueues;
    private final FailureAtomicMulticastProtocol parent;
    private final OrderedQueue orderedQueue;
    private IFlush flush;
    private boolean stabilizationPhase;
    private long flushId;

    public MessageRetransmitProtocol(IFlushParticipant flushParticipant, IMembershipManager membershipManager,  
        IMessageFactory messageFactory, IReceiver receiver, ISender sender, ITimeService timeService, boolean durable, boolean ordered,
        Map<IAddress, ReceiveQueue> receiveQueues, FailureAtomicMulticastProtocol parent, OrderedQueue orderedQueue,
        int maxUnlockQueueCapacity, int minLockQueueCapacity)
    {
        Assert.notNull(flushParticipant);
        Assert.notNull(membershipManager);
        Assert.notNull(messageFactory);
        Assert.notNull(receiver);
        Assert.notNull(sender);
        Assert.notNull(timeService);
        Assert.notNull(receiveQueues);
        Assert.notNull(parent);
        Assert.isTrue(ordered == (orderedQueue != null));
        
        this.flushParticipant = flushParticipant;
        this.membershipManager = membershipManager;
        this.messageFactory = messageFactory;
        this.receiver = receiver;
        this.sender = sender;
        this.timeService = timeService;
        this.durable = durable;
        this.ordered = ordered;
        this.receiveQueues = receiveQueues;
        this.parent = parent;
        this.orderedQueue = orderedQueue;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
    }
    
    public void setFlowController(IFlowController<IAddress> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
    }
    
    public boolean isStabilizationPhase()
    {
        return stabilizationPhase;
    }
    
    public void startFlush(IFlush flush)
    {
        this.flush = flush;
        flushId++;
        if (flush.getOldMembership() != null)
            stabilizationPhase = true;
        else
            parent.tryGrantFlush();
    }
    
    public void beforeProcessFlush()
    {
        flush.grantFlush(flushParticipant);
        stabilizationPhase = false;
    }
    
    public void endFlush()
    {
        flush = null;
    }
    
    public void onMemberFailed(INode member)
    {
        if (stabilizationPhase)
            completeStabilizationPhase(true);
    }
    
    public IExchangeData getData()
    {
        if (!stabilizationPhase)
            return null;

        List<MissingMessageInfo> missingMessageInfos = new ArrayList<MissingMessageInfo>();
        
        for (INode node : flush.getMembershipChange().getLeftMembers())
            addMissingMessageInfo(node, missingMessageInfos);
        
        for (INode node : flush.getMembershipChange().getFailedMembers())
            addMissingMessageInfo(node, missingMessageInfos);

        return new FailureAtomicExchangeData(flushId, missingMessageInfos);
    }

    public void setData(Map<INode, IExchangeData> data)
    {
        if (!stabilizationPhase)
            return;

        // TODO: переделать
        // - по каждому сбойному узлу-отправителю из missingInfo
        //  * проверяем, является ли локальный узел получателем (имеет пропуски), если нет, завершаем фазу стабилизации
        //    - получить очередь для заданного retransmitinfo, (игнорируем maxreceived == -1
        //    - если ненайдена или lastreceivedid очереди меньше maxreceived значит пропуск
        //    - сохранять мапку ретрансмитов до момента приема (если есть пропуски)
        // - учесть, что ретрансмит узлу может быть послан и принят на узле до приема setdata(race condition)
        IMembership membership = membershipManager.getMembership();
        List<RetransmitInfo> retransmits = buildRetransmitInfos(data, membership);
        for (RetransmitInfo info : retransmits)
        {
            for (RetransmitNodeInfo nodeInfo : info.retransmits)
            {
                List<IMessage> retransmittedMessages = new ArrayList<IMessage>();
                for (long i = nodeInfo.receivedMessageId + 1; i <= info.maxReceivedMessageId; i++)
                    retransmittedMessages.add(info.receiveQueue.getMessage(i));
             
                sender.send(messageFactory.create(nodeInfo.receiveNode.getAddress(),
                    new RetransmitMessagePart(info.failedNode.getId(), flushId, retransmittedMessages),
                    MessageFlags.HIGH_PRIORITY));
            }
        }
        
        // TODO: см. выше
        completeStabilizationPhase(false);
    }
    
    public boolean receive(IMessage message)
    {
        if (message.getPart() instanceof RetransmitMessagePart)
        {
            RetransmitMessagePart part = message.getPart();
            if (!stabilizationPhase || part.getFlushId() != flushId)
                return true;
            
            IMembership membership = membershipManager.getMembership();
            Assert.checkState(membership != null);
            Assert.isTrue(!part.getRetransmittedMessages().isEmpty());
            
            for (IMessage retransmittedMessage : part.getRetransmittedMessages())
            {
                FailureAtomicMessagePart failureAtomicPart = retransmittedMessage.getPart();
                
                ReceiveQueue receiveQueue = receiveQueues.get(retransmittedMessage.getSource());
                if (receiveQueue == null)
                {
                    receiveQueue = new ReceiveQueue(retransmittedMessage.getSource(), receiver, orderedQueue,
                        failureAtomicPart.getMessageId(), durable, ordered, maxUnlockQueueCapacity, minLockQueueCapacity,
                        flowController);
                    receiveQueues.put(retransmittedMessage.getSource(), receiveQueue);
                }
                
                receiveQueue.receive(failureAtomicPart.getMessageId(), failureAtomicPart.getOrder(), 
                    retransmittedMessage, timeService.getCurrentTime());
                receiveQueue.acknowledge();
            }
            
            //TODO: завершать, только если получены все недостающие, определенные на этапе setData
            completeStabilizationPhase(false);
            return true;
        }
        else
            return false;
    }
    
    private void addMissingMessageInfo(INode node, List<MissingMessageInfo> missingMessageInfos)
    {
        long lastReceivedMessageId = -1;
        ReceiveQueue receiveQueue = receiveQueues.get(node.getAddress());
        if (receiveQueue != null)
            lastReceivedMessageId = receiveQueue.getLastReceivedMessageId();
        missingMessageInfos.add(new MissingMessageInfo(node.getId(), lastReceivedMessageId));
    }
    
    private List<RetransmitInfo> buildRetransmitInfos(Map<INode, IExchangeData> exchangeData, IMembership membership)
    {
        Map<UUID, RetransmitInfo> retransmits = new HashMap<UUID, RetransmitInfo>();
        for (Map.Entry<INode, IExchangeData> entry : exchangeData.entrySet())
            buildRetransmitInfos(entry, retransmits, membership);
        
        List<RetransmitInfo> list = new ArrayList<RetransmitInfo>();
        for (RetransmitInfo info : retransmits.values())
        {
            if (info.senderNode.equals(membershipManager.getLocalNode()) && info.maxReceivedMessageId != -1)
            {
                info.receiveQueue = receiveQueues.get(info.failedNode.getAddress());
                info.buildMissing();
                if (!info.retransmits.isEmpty())
                    list.add(info);
            }
        }
        return list;
    }
    
    private void buildRetransmitInfos(Map.Entry<INode, IExchangeData> exchangeData, 
        Map<UUID, RetransmitInfo> retransmits, IMembership membership)
    {
        FailureAtomicExchangeData data = (FailureAtomicExchangeData)exchangeData.getValue();
        
        for (MissingMessageInfo missingInfo : data.getMissingMessageInfos())
        {
            RetransmitInfo retransmitInfo = retransmits.get(missingInfo.getFailedSenderId());
            if (retransmitInfo == null)
            {
                INode failedNode = flush.getOldMembership().getGroup().findMember(missingInfo.getFailedSenderId());
                retransmitInfo = new RetransmitInfo(failedNode);
                retransmits.put(missingInfo.getFailedSenderId(), retransmitInfo);
            }
            
            if (retransmitInfo.maxReceivedMessageId < missingInfo.getLastReceivedMessageId())
            {
                retransmitInfo.maxReceivedMessageId = missingInfo.getLastReceivedMessageId();
                retransmitInfo.senderNode = exchangeData.getKey();
            }
            
            retransmitInfo.retransmits.add(new RetransmitNodeInfo(exchangeData.getKey(), missingInfo.getLastReceivedMessageId()));
        }
    }
    
    private void completeStabilizationPhase(boolean failure)
    {
        stabilizationPhase = false;
        
        if (failure)
            flush.grantFlush(flushParticipant);
        else
            parent.tryGrantFlush();
    }
    
    private static class RetransmitInfo
    {
        private final INode failedNode;
        private INode senderNode;
        private long maxReceivedMessageId = -1;
        private ReceiveQueue receiveQueue;
        private List<RetransmitNodeInfo> retransmits;
        
        public RetransmitInfo(INode failedNode)
        {
            Assert.notNull(failedNode);
            
            this.failedNode = failedNode;
        }
        
        public void buildMissing()
        {
            List<RetransmitNodeInfo> retransmits = new ArrayList<RetransmitNodeInfo>();
            for (RetransmitNodeInfo info : this.retransmits)
            {
                if (info.receivedMessageId == -1)
                    info.receivedMessageId = receiveQueue.getLastAcknowledgedMessageId();
                if (info.receivedMessageId < maxReceivedMessageId)
                    retransmits.add(info);
            }
            
            this.retransmits = retransmits;
        }
    }
    
    private static class RetransmitNodeInfo
    {
        private final INode receiveNode;
        private long receivedMessageId;
        
        public RetransmitNodeInfo(INode receiveNode, long receivedMessageId)
        {
            this.receiveNode = receiveNode;
            this.receivedMessageId = receivedMessageId;
        }
        
        @Override
        public String toString()
        {
            return receiveNode + ":" + receivedMessageId;
        }
    }
}

