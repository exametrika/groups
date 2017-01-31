/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.exchange.IExchangeData;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.membership.IMembershipDelta;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.membership.Memberships.MembershipChangeInfo;

/**
 * The {@link FlushParticipantProtocol} represents a protocol that participates in flush.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FlushParticipantProtocol extends AbstractProtocol implements IGracefulCloseStrategy
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IFlushParticipant> participants;
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;
    private IMembership preparedMembership;
    private IMembershipDelta preparedMembershipDelta;
    private Flush flush;
    private Set<IFlushParticipant> notGrantedParticipants;
    private Phase phase = Phase.READY;
    private boolean flushProcessingRequired;
    
    public enum Phase
    {
        READY, STABILIZE, PROCESS 
    }

    public FlushParticipantProtocol(String channelName, IMessageFactory messageFactory, List<IFlushParticipant> participants, 
        IMembershipManager membershipManager, IFailureDetector failureDetector)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(participants);
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);

        this.participants = participants;
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
    }

    public List<IFlushParticipant> getParticipants()
    {
        return participants;
    }
    
    @Override
    public boolean requestClose()
    {
        return phase == Phase.READY;
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof FlushStartMessagePart)
        {
            boolean started = false;
            FlushStartMessagePart part = message.getPart();

            if (preparedMembership == null)
            {
                IMembershipChange membershipChange = null;
                if (membershipManager.getMembership() == null)
                {
                    Assert.notNull(part.getPreparedMembership());
                    Assert.isNull(part.getPreparedMembershipDelta());
                    preparedMembership = part.getPreparedMembership();
                }
                else
                {
                    Assert.notNull(part.getPreparedMembershipDelta());
                    Assert.isNull(part.getPreparedMembership());
                    preparedMembershipDelta = part.getPreparedMembershipDelta();
                    
                    MembershipChangeInfo info = Memberships.createMembership(membershipManager.getMembership(), preparedMembershipDelta);
                    preparedMembership = info.newMembership;
                    membershipChange = info.membershipChange;
                }
                
                flush = new Flush(part.isGroupForming(), membershipManager.getMembership(), preparedMembership, membershipChange);
                started = true;
            }
            
            phase = Phase.STABILIZE;

            notGrantedParticipants = new HashSet<IFlushParticipant>(participants);

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());
            
            flushProcessingRequired = false;
            for (IFlushParticipant participant : participants)
            {
                if (participant.isFlushProcessingRequired())
                {
                    flushProcessingRequired = true;
                    break;
                }
            }

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.startFlush(Strings.wrap(flush.toString(), 4, 120)));

            if (started)
            {
                if (membershipManager.getMembership() == null)
                    membershipManager.prepareInstallMembership(preparedMembership);
                else
                    membershipManager.prepareChangeMembership(preparedMembership, flush.getMembershipChange());
            }

            for (IFlushParticipant participant : participants)
                participant.startFlush(flush);
            
            List<IExchangeData> exchanges = new ArrayList<IExchangeData>();
            for (IFlushParticipant participant : participants)
            {
                if (participant instanceof IExchangeableFlushParticipant)
                    exchanges.add(((IExchangeableFlushParticipant)participant).getLocalData());
                else
                    exchanges.add(null);
            }
            send(messageFactory.create(message.getSource(), new FlushExchangeGetMessagePart( 
                getNodesIds(failureDetector.getFailedMembers()), getNodesIds(failureDetector.getLeftMembers()), exchanges), 
                MessageFlags.HIGH_PRIORITY));
        }
        else if (message.getPart() instanceof FlushMessagePart && 
            ((FlushMessagePart)message.getPart()).getType() == FlushMessagePart.Type.PROCESS)
        {
            FlushMessagePart part = message.getPart();

            notGrantedParticipants = new HashSet<IFlushParticipant>(participants);
            phase = Phase.PROCESS;
            flushProcessingRequired = false;

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.processFlush());

            for (IFlushParticipant participant : participants)
            {
                if (participant.isFlushProcessingRequired())
                    participant.beforeProcessFlush();
            }
            
            for (IFlushParticipant participant : participants)
            {
                if (participant.isFlushProcessingRequired())
                    participant.processFlush();
                else
                    grantFlush(participant);
            }
        }
        else if (message.getPart() instanceof FlushMessagePart && 
            ((FlushMessagePart)message.getPart()).getType() == FlushMessagePart.Type.END)
        {
            FlushMessagePart part = message.getPart();

            if (flush == null)
            {
                send(messageFactory.create(message.getSource(), new FlushResponseMessagePart(flushProcessingRequired, 
                    getNodesIds(failureDetector.getFailedMembers()), getNodesIds(failureDetector.getLeftMembers())), 
                    MessageFlags.HIGH_PRIORITY));
                return;
            }

            flush.close();
            flush = null;
            phase = Phase.READY;
            flushProcessingRequired = false;
            preparedMembership = null;
            preparedMembershipDelta = null;

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());

            membershipManager.commitMembership();

            for (IFlushParticipant participant : participants)
                participant.endFlush();

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.completeFlush());

            send(messageFactory.create(message.getSource(), new FlushResponseMessagePart(flushProcessingRequired, 
                getNodesIds(failureDetector.getFailedMembers()), getNodesIds(failureDetector.getLeftMembers())), 
                MessageFlags.HIGH_PRIORITY));
        }
        else if (message.getPart() instanceof FlushMessagePart &&
            ((FlushMessagePart)message.getPart()).getType() == FlushMessagePart.Type.REQUEST_STATE)
        {
            FlushMessagePart part = message.getPart();

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());

            List<Object> coordinatorStates = getCoordinatorStates();
            Set<UUID> failedNodeIds = getNodesIds(failureDetector.getFailedMembers());
            Set<UUID> leftNodeIds = getNodesIds(failureDetector.getLeftMembers());
            
            FlushStateResponseMessagePart responsePart;
            if (phase != Phase.READY && preparedMembershipDelta != null)
                responsePart = new FlushStateResponseMessagePart(phase, null, preparedMembershipDelta, flushProcessingRequired,
                    failedNodeIds, leftNodeIds, coordinatorStates);
            else
                responsePart = new FlushStateResponseMessagePart(phase, preparedMembership, null, flushProcessingRequired,
                    failedNodeIds, leftNodeIds, coordinatorStates);
            
            send(messageFactory.create(message.getSource(), responsePart, MessageFlags.HIGH_PRIORITY));
        }
        else if (message.getPart() instanceof FlushExchangeSetMessagePart)
        {
            if (phase != Phase.STABILIZE)
                return;
            
            FlushExchangeSetMessagePart part = message.getPart();
            for (int i = 0; i < participants.size(); i++)
            {
                IFlushParticipant participant = participants.get(i);
                if (participant instanceof IExchangeableFlushParticipant)
                    ((IExchangeableFlushParticipant)participant).setRemoteData(getDataExchanges(flush.getNewMembership(),
                        part.getDataExchanges().get(i)));
            }
        }
        else
            receiver.receive(message);
    }

    private Map<INode, IExchangeData> getDataExchanges(IMembership membership, Map<UUID, IExchangeData> map)
    {
        Map<INode, IExchangeData> dataExchanges = new HashMap<INode, IExchangeData>();
        for (Map.Entry<UUID, IExchangeData> entry : map.entrySet())
        {
            INode member = membership.getGroup().findMember(entry.getKey());
            Assert.notNull(member);
            
            dataExchanges.put(member, entry.getValue());
        }
        return dataExchanges;
    }

    private List<Object> getCoordinatorStates()
    {
        List<Object> coordinatorStates = new ArrayList<Object>();
        for (int i = 0; i < participants.size(); i++)
        {
            IFlushParticipant participant = participants.get(i);
            if (participant instanceof IFlushParticipantWithCoordinatorState &&
                ((IFlushParticipantWithCoordinatorState)participant).isCoordinatorStateSupported())
                coordinatorStates.add(((IFlushParticipantWithCoordinatorState)participant).getCoordinatorState());
            else
                coordinatorStates.add(null);
        }

        return coordinatorStates;
    }

    private void grantFlush(IFlushParticipant participant)
    {
        Assert.isTrue(notGrantedParticipants.remove(participant));

        if (notGrantedParticipants.isEmpty())
            grantFlush();
    }
    
    private void grantFlush()
    {
        notGrantedParticipants = null;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.grantFlush());
        
        send(messageFactory.create(failureDetector.getCurrentCoordinator().getAddress(),
            new FlushResponseMessagePart(flushProcessingRequired, getNodesIds(failureDetector.getFailedMembers()),
            getNodesIds(failureDetector.getLeftMembers())), MessageFlags.HIGH_PRIORITY));
    }
    
    private Set<UUID> getNodesIds(Set<INode> nodes)
    {
        Set<UUID> ids = new HashSet<UUID>();
        
        for (INode node : nodes)
            ids.add(node.getId());
        
        return ids;
    }

    private class Flush implements IFlush
    {
        private final boolean groupForming;
        private final IMembership oldMembership;
        private final IMembership newMembership;
        private final IMembershipChange membershipChange;
        private boolean closed;

        public Flush(boolean groupForming, IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
        {
            this.groupForming = groupForming;
            this.oldMembership = oldMembership;
            this.newMembership = newMembership;
            this.membershipChange = membershipChange;
        }

        public void close()
        {
            closed = true;
        }
        
        @Override
        public boolean isGroupForming()
        {
            return groupForming;
        }
        
        @Override
        public IMembership getOldMembership()
        {
            return oldMembership;
        }

        @Override
        public IMembership getNewMembership()
        {
            return newMembership;
        }

        @Override
        public IMembershipChange getMembershipChange()
        {
            return membershipChange;
        }
        
        @Override
        public void grantFlush(IFlushParticipant participant)
        {
            if (!closed)
                FlushParticipantProtocol.this.grantFlush(participant);
        }
        
        @Override
        public String toString()
        {
            return messages.flushToString(Strings.wrap(oldMembership != null ? oldMembership.toString() : messages.notSet().toString(), 4, 120),
                Strings.wrap(newMembership.toString(), 4, 120), 
                Strings.wrap(membershipChange != null ? membershipChange.toString() : messages.notSet().toString(), 4, 120)).toString();
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Flush has been started:\n{0}.")
        ILocalizedMessage startFlush(String flush);
        @DefaultMessage("Old membership:\n{0}\nNew membership:\n{1}\nChange:\n{2}")
        ILocalizedMessage flushToString(String oldMembership, String newMembership, String membershipChange);
        @DefaultMessage("Processing phase of flush has been started.")
        ILocalizedMessage processFlush();
        @DefaultMessage("Flush completed.")
        ILocalizedMessage completeFlush();
        @DefaultMessage("Flush granted.")
        ILocalizedMessage grantFlush();
        @DefaultMessage("(not set)")
        ILocalizedMessage notSet();
    }
}
