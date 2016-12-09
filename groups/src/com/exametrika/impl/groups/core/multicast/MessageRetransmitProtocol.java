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
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
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
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IFlushParticipant flushParticipant;
    private final IMembershipManager membershipManager;
    private final ILogger logger;
    private final IMessageFactory messageFactory;
    private final IReceiver receiver;
    private final ISender sender;
    private final ITimeService timeService;
    private final boolean durable;
    private final boolean ordered;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private final IFlowController<IAddress> flowController;
    private final Map<IAddress, ReceiveQueue> receiveQueues;
    private final FailureAtomicMulticastProtocol parent;
    private final OrderedQueue orderedQueue;
    private IFlush flush;
    private boolean stabilizationPhase;
    private boolean exchangeDataSent;
    private Map<INode, FailureAtomicExchangeData> exchangeData;
    private Map<Pair<UUID, UUID>, RetransmitNodeInfo> retransmits;
    private int unacknowledgedRetransmitCount;
    private long flushId;

    public MessageRetransmitProtocol(IFlushParticipant flushParticipant, IMembershipManager membershipManager, ILogger logger, 
        IMessageFactory messageFactory, IReceiver receiver, ISender sender, ITimeService timeService, boolean durable, boolean ordered,
        Map<IAddress, ReceiveQueue> receiveQueues, FailureAtomicMulticastProtocol parent, OrderedQueue orderedQueue,
        int maxUnlockQueueCapacity, int minLockQueueCapacity, IFlowController<IAddress> flowController)
    {
        Assert.notNull(flushParticipant);
        Assert.notNull(membershipManager);
        Assert.notNull(logger);
        Assert.notNull(messageFactory);
        Assert.notNull(receiver);
        Assert.notNull(sender);
        Assert.notNull(timeService);
        Assert.notNull(receiveQueues);
        Assert.notNull(parent);
        Assert.isTrue(ordered == (orderedQueue != null));
        Assert.notNull(flowController);
        
        this.flushParticipant = flushParticipant;
        this.membershipManager = membershipManager;
        this.logger = logger;
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
        stabilizationPhase = true;
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
        if (!stabilizationPhase || exchangeDataSent)
            return null;

        List<MissingMessageInfo> missingMessageInfos = new ArrayList<MissingMessageInfo>();
        
        for (INode node : flush.getMembershipChange().getLeftMembers())
            addMissingMessageInfo(node, missingMessageInfos);
        
        for (INode node : flush.getMembershipChange().getFailedMembers())
            addMissingMessageInfo(node, missingMessageInfos);

        exchangeDataSent = true;
        FailureAtomicExchangeData data = new FailureAtomicExchangeData(missingMessageInfos);
        
        if (exchangeData == null)
            exchangeData = new HashMap<INode, FailureAtomicExchangeData>();
        
        exchangeData.put(membershipManager.getLocalNode(), data);
        
        return data;
    }

    public void setData(INode source, Object data)
    {
        if (exchangeData == null)
            exchangeData = new HashMap<INode, FailureAtomicExchangeData>();
        
        exchangeData.put(source, (FailureAtomicExchangeData)data);
    }
    
    public void onCycleCompleted()
    {
        if (stabilizationPhase && exchangeDataSent)
        {
            IMembership membership = membershipManager.getMembership();
            List<RetransmitInfo> retransmits = buildRetransmitInfos(exchangeData, membership);
            if (retransmits.isEmpty())
                completeStabilizationPhase(false);
            else
            {
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
                        
                        unacknowledgedRetransmitCount++;
                        this.retransmits.put(new Pair<UUID, UUID>(info.failedNode.getId(), nodeInfo.receiveNode.getId()), nodeInfo);
                    }
                }
            }
        }
        
        exchangeData = null;
    }
    
    public boolean receive(IMessage message)
    {
        if (message.getPart() instanceof RetransmitMessagePart)
        {
            RetransmitMessagePart part = message.getPart();
            if (part.getFlushId() != flushId)
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
            
            sender.send(messageFactory.create(message.getSource(), new AcknowledgeRetransmitMessagePart(
                part.getFailedNodeId(), part.getFlushId()), MessageFlags.HIGH_PRIORITY));
            return true;
        }
        else if (message.getPart() instanceof AcknowledgeRetransmitMessagePart)
        {
            AcknowledgeRetransmitMessagePart part = message.getPart();
            if (part.getFlushId() != flushId)
                return true;
            
            RetransmitNodeInfo info = retransmits.get(new Pair<UUID, UUID>(part.getFailedNodeId(), message.getSource().getId()));
            if (info != null && !info.acknowledged)
            {
                info.acknowledged = true;
                unacknowledgedRetransmitCount--;
                
                if (unacknowledgedRetransmitCount == 0)
                    completeStabilizationPhase(false);
            }
            return true;
        }
        else
            return false;
    }
    
    private void addMissingMessageInfo(INode node, List<MissingMessageInfo> missingMessageInfos)
    {
        ReceiveQueue receiveQueue = receiveQueues.get(node.getAddress());
        if (receiveQueue == null)
            return;
        
        long lastReceivedMessageId = receiveQueue.getLastReceivedMessageId();
        missingMessageInfos.add(new MissingMessageInfo(node.getId(), lastReceivedMessageId));
    }
    
    private List<RetransmitInfo> buildRetransmitInfos(Map<INode, FailureAtomicExchangeData> exchangeData, IMembership membership)
    {
        Map<UUID, RetransmitInfo> retransmits = new HashMap<UUID, RetransmitInfo>();
        for (Map.Entry<INode, FailureAtomicExchangeData> entry : exchangeData.entrySet())
            buildRetransmitInfos(entry, retransmits, membership);
        
        List<RetransmitInfo> list = new ArrayList<RetransmitInfo>();
        for (RetransmitInfo info : retransmits.values())
        {
            if (info.maxBufferedMessageId < info.maxReceivedMessageId)
            {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.replicasDiscrepancyDetected(info.maxBufferedMessageId, info.getDivergedInfos()));
            }
            
            if (info.senderNode.equals(membershipManager.getLocalNode()))
            {
                info.receiveQueue = receiveQueues.get(info.failedNode.getAddress());
                info.buildMissing();
                list.add(info);
            }
        }
        return list;
    }
    
    private void buildRetransmitInfos(Map.Entry<INode, FailureAtomicExchangeData> exchangeData, 
        Map<UUID, RetransmitInfo> retransmits, IMembership membership)
    {
        FailureAtomicExchangeData data = exchangeData.getValue();
        
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
                retransmitInfo.maxReceivedMessageId = missingInfo.getLastReceivedMessageId();
            
            if (retransmitInfo.maxBufferedMessageId < missingInfo.getLastReceivedMessageId())
            {
                retransmitInfo.maxBufferedMessageId = missingInfo.getLastReceivedMessageId();
                retransmitInfo.senderNode = exchangeData.getKey();
            }
            else if (retransmitInfo.maxBufferedMessageId == missingInfo.getLastReceivedMessageId()) 
            {
                Assert.notNull(retransmitInfo.senderNode);
                int rank1 = membership.getGroup().getMembers().indexOf(retransmitInfo.senderNode);
                int rank2 = membership.getGroup().getMembers().indexOf(exchangeData.getKey());
                if (rank2 < rank1)
                    retransmitInfo.senderNode = exchangeData.getKey();
            }
            
            retransmitInfo.retransmits.add(new RetransmitNodeInfo(exchangeData.getKey(), missingInfo.getLastReceivedMessageId()));
        }
    }
    
    private void completeStabilizationPhase(boolean failure)
    {
        exchangeDataSent = false;
        exchangeData = null;
        stabilizationPhase = false;
        retransmits = null;
        unacknowledgedRetransmitCount = 0;
        
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
        private long maxBufferedMessageId = -1;
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
                if (info.receivedMessageId < maxReceivedMessageId)
                    retransmits.add(info);
            }
            
            this.retransmits = retransmits;
        }
        
        public List<RetransmitNodeInfo> getDivergedInfos()
        {
            List<RetransmitNodeInfo> list = new ArrayList<RetransmitNodeInfo>();
            for (RetransmitNodeInfo info : retransmits)
            {
                if (info.receivedMessageId > maxBufferedMessageId)
                    list.add(info);
            }
            
            return list;
        }
    }
    
    private static class RetransmitNodeInfo
    {
        private final INode receiveNode;
        private final long receivedMessageId;
        private boolean acknowledged;
        
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
    
    private interface IMessages
    {
        @DefaultMessage("Replicas discrepancy due to loss of all buffering nodes has been detected. Available message id: {0}, diverged nodes: {1}.")
        ILocalizedMessage replicasDiscrepancyDetected(long bufferedMessageId, List<RetransmitNodeInfo> divergedInfos);
    }
}

