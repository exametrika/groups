/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.feedback.DataLossState;
import com.exametrika.impl.groups.cluster.feedback.IDataLossFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.IDataLossState;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;

/**
 * The {@link GroupCheckStateProtocol} represents a checking equality of group state protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class GroupCheckStateProtocol extends AbstractProtocol implements IFailureDetectionListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipManager membershipManager;
    private final IGroupStateChecksumProvider stateChecksumProvider;
    private final IDataLossFeedbackService dataLossFeedbackService;
    private final long checkStatePeriod;
    private final UUID groupId;
    private final GroupAddress groupAddress;
    private final String groupDomain;
    private IGroupFailureDetector failureDetector;
    private Set<IAddress> respondingNodes = new HashSet<IAddress>();
    private Long checksum;
    private long lastCheckTime;
    
    public GroupCheckStateProtocol(String channelName, IMessageFactory messageFactory, 
        IGroupMembershipManager membershipManager, IGroupStateChecksumProvider stateChecksumProvider,
        IDataLossFeedbackService dataLossFeedbackService, long checkStatePeriod, UUID groupId,
        GroupAddress groupAddress, String groupDomain)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(stateChecksumProvider);
        Assert.notNull(dataLossFeedbackService);
        Assert.notNull(groupId);
        Assert.notNull(groupAddress);
        Assert.notNull(groupDomain);
        
        this.membershipManager = membershipManager;
        this.stateChecksumProvider = stateChecksumProvider;
        this.dataLossFeedbackService = dataLossFeedbackService;
        this.checkStatePeriod = checkStatePeriod;
        this.groupId = groupId;
        this.groupAddress = groupAddress;
        this.groupDomain = groupDomain;
    }
    
    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new StateChecksumResponseMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(StateChecksumResponseMessagePartSerializer.ID);
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        respondingNodes.remove(member.getAddress());
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (currentTime > lastCheckTime + checkStatePeriod &&
            membershipManager.getLocalNode().equals(failureDetector.getCurrentCoordinator()) &&
            com.exametrika.common.utils.Collections.isEmpty(respondingNodes))
        {
            lastCheckTime = currentTime;
            checksum = null;
            List<INode> healthyNodes = failureDetector.getHealthyMembers();
            respondingNodes = new HashSet<IAddress>();
            for (INode node : healthyNodes)
                respondingNodes.add(node.getAddress());
            
            send(messageFactory.create(groupAddress, MessageFlags.STATE_CHECKSUM_REQUEST));
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof StateChecksumResponseMessagePart)
        {
            StateChecksumResponseMessagePart part = message.getPart();
            if (checksum == null)
                checksum = part.getChecksum();
            else if (checksum != part.getChecksum())
            {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, messages.stateNotEqual(message.getSource()));
                
                IDataLossState state = new DataLossState(groupDomain, groupId);
                dataLossFeedbackService.updateDataLossState(state);
            }
            
            respondingNodes.remove(message.getSource());
        }
        else if (message.hasFlags(MessageFlags.STATE_CHECKSUM_REQUEST))
            send(messageFactory.create(message.getSource(), new StateChecksumResponseMessagePart(stateChecksumProvider.getStateChecksum())));
        else
            receiver.receive(message);
    }

    private interface IMessages
    {
        @DefaultMessage("Group state of node ''{0}'' differs from other group nodes.")
        ILocalizedMessage stateNotEqual(IAddress respondingNode);
    }
}
