/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.exchange.IExchangeData;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
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
    private final ISender sender;
    private final ITimeService timeService;
    private IFlowController<IAddress> flowController;
    private final Map<IAddress, ReceiveQueue> receiveQueues;
    private final FailureAtomicMulticastProtocol parent;
    private final IFailureDetector failureDetector;
    private IFlush flush;
    private boolean stabilizationPhase;
    private long flushId;
    private Set<UUID> retransmits;
    
    public MessageRetransmitProtocol(IFlushParticipant flushParticipant, IMembershipManager membershipManager,  
        IMessageFactory messageFactory, ISender sender, ITimeService timeService,
        Map<IAddress, ReceiveQueue> receiveQueues, FailureAtomicMulticastProtocol parent, IFailureDetector failureDetector)
    {
        
        Assert.notNull(flushParticipant);
        Assert.notNull(membershipManager);
        Assert.notNull(messageFactory);
        Assert.notNull(sender);
        Assert.notNull(timeService);
        Assert.notNull(receiveQueues);
        Assert.notNull(parent);
        Assert.notNull(failureDetector);
        
        this.flushParticipant = flushParticipant;
        this.membershipManager = membershipManager;
        this.messageFactory = messageFactory;
        this.sender = sender;
        this.timeService = timeService;
        this.receiveQueues = receiveQueues;
        this.parent = parent;
        this.failureDetector = failureDetector;
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
        retransmits = null;
        if (flush.getOldMembership() != null)
            stabilizationPhase = true;
        else
            parent.tryGrantFlush();
    }
    
    public void beforeProcessFlush()
    {
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
        IMembership membership = flush.getOldMembership();
        for (INode node : membership.getGroup().getMembers())
        {
            if (!failureDetector.isHealthyMember(node.getId()))
                addMissingMessageInfo(node, missingMessageInfos);
        }

        return new FailureAtomicExchangeData(flushId, missingMessageInfos);
    }

    public void setData(Map<INode, IExchangeData> data)
    {
        if (!stabilizationPhase)
            return;

        IMembership membership = membershipManager.getMembership();
        Assert.checkState(membership != null);
        
        Map<UUID, RetransmitInfo> retransmitsMap = new HashMap<UUID, RetransmitInfo>();
        for (Map.Entry<INode, IExchangeData> entry : data.entrySet())
        {
            if (entry.getValue() != null)
                buildRetransmitInfos(entry, retransmitsMap, membership);
        }
        
        Set<UUID> retransmits = new HashSet<UUID>();
        for (RetransmitInfo info : retransmitsMap.values())
        {
            if (info.maxReceivedMessageId == -1)
                continue;
            
            ReceiveQueue receiveQueue = receiveQueues.get(info.failedNode.getAddress());
            if (info.senderNode.equals(membershipManager.getLocalNode()))
            {
                Assert.notNull(receiveQueue);
                
                for (RetransmitNodeInfo nodeInfo : info.retransmits)
                {
                    if (nodeInfo.receivedMessageId == -1)
                        nodeInfo.receivedMessageId = receiveQueue.getLastAcknowledgedMessageId();
                    
                    if (nodeInfo.receivedMessageId < info.maxReceivedMessageId)
                    {
                        List<IMessage> retransmittedMessages = new ArrayList<IMessage>();
                        for (long i = nodeInfo.receivedMessageId + 1; i <= info.maxReceivedMessageId; i++)
                            retransmittedMessages.add(receiveQueue.getMessage(i));
                     
                        sender.send(messageFactory.create(nodeInfo.receiveNode.getAddress(),
                            new RetransmitMessagePart(info.failedNode.getId(), flushId, retransmittedMessages),
                            MessageFlags.HIGH_PRIORITY));
                    }
                }
            }
            else if (receiveQueue == null || receiveQueue.getLastReceivedMessageId() < info.maxReceivedMessageId)
                retransmits.add(info.failedNode.getId());
        }
        
        if (!retransmits.isEmpty())
            this.retransmits = retransmits;
        else
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
            
            INode failedNode = membership.getGroup().findMember(part.getFailedNodeId());
            FailureAtomicMessagePart firstPart = part.getRetransmittedMessages().get(0).getPart();
            ReceiveQueue receiveQueue = parent.ensureReceiveQueue(failedNode.getAddress(), firstPart.getMessageId());
            
            for (IMessage retransmittedMessage : part.getRetransmittedMessages())
            {
                FailureAtomicMessagePart failureAtomicPart = retransmittedMessage.getPart();
                receiveQueue.receive(failureAtomicPart.getMessageId(), failureAtomicPart.getOrder(), 
                    retransmittedMessage, timeService.getCurrentTime());
                receiveQueue.acknowledge();
            }
            
            if (retransmits != null)
            {
                retransmits.remove(part.getFailedNodeId());
                if (retransmits.isEmpty())
                    completeStabilizationPhase(false);
            }            
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
            else if (retransmitInfo.maxReceivedMessageId == missingInfo.getLastReceivedMessageId()) 
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
        stabilizationPhase = false;
        retransmits = null;
        
        if (failure)
            flush.grantFlush(flushParticipant);
        else
            parent.tryGrantFlush();
    }
    
    private static class RetransmitInfo
    {
        private final INode failedNode;
        private INode senderNode;
        private long maxReceivedMessageId = -2;
        private List<RetransmitNodeInfo> retransmits = new ArrayList<RetransmitNodeInfo>();
        
        public RetransmitInfo(INode failedNode)
        {
            Assert.notNull(failedNode);
            
            this.failedNode = failedNode;
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

