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
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.membership.IMembershipDelta;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipSerializationRegistrar;
import com.exametrika.impl.groups.core.membership.Memberships;

/**
 * The {@link FlushCoordinatorProtocol} represents a protocol that coordinates flush.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FlushCoordinatorProtocol extends AbstractProtocol implements IFlushManager, IGracefulCloseStrategy, 
    IFailureDetectionListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;
    private final long flushTimeout;
    private final FlushParticipantProtocol participantProtocol;
    private boolean flushCoordinator;
    private boolean phaseRestartRequired;
    private boolean flushProcessingRequired;
    private Phase phase = Phase.READY;
    private Set<IAddress> respondingMembers = new HashSet<IAddress>();
    private Set<IAddress> processingMembers = new HashSet<IAddress>();
    private Map<IAddress, FlushStateResponseMessagePart> flushStateResponses = new HashMap<IAddress, FlushStateResponseMessagePart>();
    private IMembership installingMembership;
    private IMembershipDelta installingMembershipDelta;
    private long startWaitTime;
    private boolean stopped;
    
    public enum Phase
    {
        UNKNOWN, READY, START, STABILIZE, PROCESS, END
    }

    public FlushCoordinatorProtocol(String channelName, IMessageFactory messageFactory, IMembershipManager membershipManager,
        IFailureDetector failureDetector, long flushTimeout, FlushParticipantProtocol participantProtocol)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(participantProtocol);

        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.flushTimeout = flushTimeout;
        this.participantProtocol = participantProtocol;
    }

    @Override
    public boolean isFlushInProgress()
    {
        if (!flushCoordinator)
            return false;
        
        return phase != Phase.READY;
    }

    @Override
    public void install(IMembership membership, IMembershipDelta membershipDelta)
    {
        Assert.notNull(membership);
        Assert.checkState(phase == Phase.READY);
        Assert.isTrue(!stopped);

        IMembership currentMembership = membershipManager.getMembership();
        
        if (currentMembership != null)
        {
            Assert.notNull(membershipDelta);
            Assert.isTrue(membership.getId() == currentMembership.getId() + 1);
            Assert.isTrue(membershipDelta.getId() == membership.getId());
            
            if (!flushCoordinator && !currentMembership.getGroup().getCoordinator().equals(membershipManager.getLocalNode()))
                throw new UnsupportedOperationException();
        }
        else
            Assert.isNull(membershipDelta);
        
        Assert.isTrue(membership.getGroup().getCoordinator().equals(membershipManager.getLocalNode()));

        flushCoordinator = true;
        
        installingMembership = membership;
        installingMembershipDelta = membershipDelta;
        phase = Phase.START;
        flushProcessingRequired = false;
        processingMembers.clear();

        for (IFlushParticipant participant : participantProtocol.getParticipants())
            participant.setCoordinator();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.membershipInstalling(Strings.wrap(membership.toString(), 4, 120)));
        
        proceed();
    }

    @Override
    public void onMemberFailed(INode member)
    {
        onMemberLeft(member.getAddress(), true);
    }
    
    @Override
    public void onMemberLeft(INode member)
    {
        onMemberLeft(member.getAddress(), false);
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FlushStartMessagePartSerializer());
        registry.register(new FlushResponseMessagePartSerializer());
        registry.register(new FlushMessagePartSerializer());
        registry.register(new FlushStateResponseMessagePartSerializer());
        registry.register(new MembershipSerializationRegistrar());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FlushStartMessagePartSerializer.ID);
        registry.unregister(FlushResponseMessagePartSerializer.ID);
        registry.unregister(FlushMessagePartSerializer.ID);
        registry.unregister(FlushStateResponseMessagePartSerializer.ID);
        registry.unregister(new MembershipSerializationRegistrar());
    }

    @Override
    public boolean requestClose()
    {
        if (!isFlushInProgress())
        {
            stopped = true;
            return true;
        }
        else
            return false;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!flushCoordinator || stopped)
            return;
        
        Set<UUID> memberIds = null;

        if (startWaitTime != 0 && timeService.getCurrentTime() > startWaitTime + flushTimeout && !respondingMembers.isEmpty())
            memberIds = getNodeIdsByAddress(respondingMembers);
        
        if (memberIds != null)
            failureDetector.addFailedMembers(memberIds);
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof FlushResponseMessagePart)
        {
            FlushResponseMessagePart part = message.getPart();

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());

            if (!flushCoordinator || phase == Phase.READY)
                return;

            if (!respondingMembers.contains(message.getSource()))
                return;
            
            if (part.isFlushProcessingRequired())
            {
                flushProcessingRequired = true;
                processingMembers.add(message.getSource());
            }
            
            respondingMembers.remove(message.getSource());
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.responseReceived(message.getSource(), 
                    Strings.wrap(respondingMembers.toString(), 4, 120)));
            
            if (respondingMembers.isEmpty())
                proceed();
        }
        else if (message.getPart() instanceof FlushStateResponseMessagePart)
        {
            FlushStateResponseMessagePart part = message.getPart();

            if (!part.getFailedMembers().isEmpty())
                failureDetector.addFailedMembers(part.getFailedMembers());
            if (!part.getLeftMembers().isEmpty())
                failureDetector.addLeftMembers(part.getLeftMembers());

            if (!flushCoordinator || phase != Phase.UNKNOWN)
                return;

            if (!respondingMembers.contains(message.getSource()))
                return;

            Assert.isTrue(participantProtocol.getParticipants().size() == part.getCoordinatorStates().size());
            
            if (part.isFlushProcessingRequired())
            {
                flushProcessingRequired = true;
                processingMembers.add(message.getSource());
            }
            
            respondingMembers.remove(message.getSource());
            flushStateResponses.put(message.getSource(), part);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.responseReceived(message.getSource(), Strings.wrap(respondingMembers.toString(), 4, 120)));
            
            if (respondingMembers.isEmpty())
                proceedWithFlushStateResponses();
        }
        else
            receiver.receive(message);
    }

    private void onMemberLeft(IAddress member, boolean failed)
    {
        if (stopped)
            return;
        
        if (!flushCoordinator)
        {
            if (!membershipManager.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
                return;

            flushCoordinator = true;
            phase = Phase.UNKNOWN;
            if (installingMembership == null)
                installingMembership = membershipManager.getPreparedMembership();
            if (installingMembership == null)
                installingMembership = membershipManager.getMembership();
            
            for (IFlushParticipant participant : participantProtocol.getParticipants())
                participant.setCoordinator();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.flushCoordinatorChanged());
            
            proceed();
            return;
        }

        if (respondingMembers.isEmpty() || !respondingMembers.contains(member))
            return;

        phaseRestartRequired = true;
        respondingMembers.remove(member);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, failed ? messages.memberFailed(member, Strings.wrap(respondingMembers.toString(), 4, 120)) :
                messages.memberLeft(member, Strings.wrap(respondingMembers.toString(), 4, 120)));
        
        if (respondingMembers.isEmpty())
        {
            if (phase == Phase.UNKNOWN)
                proceedWithFlushStateResponses();
            else
                proceed();
        }
    }

    private void buildCoordinatorState()
    {
        List<IFlushParticipant> participants = participantProtocol.getParticipants();
        for (int i = 0; i < participants.size(); i++)
        {
            IFlushParticipant participant = participants.get(i);
            List<Object> states = new ArrayList<Object>();
            
            for (FlushStateResponseMessagePart response : flushStateResponses.values())
                states.add(response.getCoordinatorStates().get(i));
            
            if (participant.isCoordinatorStateSupported())
                participant.setCoordinatorState(states);
        }
    }
    
    private void proceedWithFlushStateResponses()
    {
        if (stopped)
            return;
        
        buildCoordinatorState();
        
        IMembership installingMembership = null;
        IMembershipDelta installingMembershipDelta = null;
        Phase coordinatorPhase = null;
        Set<FlushParticipantProtocol.Phase> phases = Enums.noneOf(FlushParticipantProtocol.Phase.class);
        for (FlushStateResponseMessagePart response : flushStateResponses.values())
        {
            if (response.getPreparedMembership() != null)
            {
                Assert.isTrue(installingMembership == null || installingMembership.equals(response.getPreparedMembership()));
                installingMembership = response.getPreparedMembership();
            }
            else if (response.getPreparedMembershipDelta() != null)
            {
                Assert.isTrue(installingMembershipDelta == null || 
                    installingMembershipDelta.getId() == response.getPreparedMembershipDelta().getId());
                installingMembershipDelta = response.getPreparedMembershipDelta();
            }

            phases.add(response.getPhase());
        }

        Assert.checkState(phases.size() >= 1 && phases.size() <= 2);

        if (phases.equals(Enums.of(FlushParticipantProtocol.Phase.READY)))
            coordinatorPhase = Phase.READY;
        else if (phases.equals(Enums.of(FlushParticipantProtocol.Phase.STABILIZE)) || 
            phases.equals(Enums.of(FlushParticipantProtocol.Phase.READY, FlushParticipantProtocol.Phase.STABILIZE)))
            coordinatorPhase = Phase.START;
        else if (phases.equals(Enums.of(FlushParticipantProtocol.Phase.PROCESS)) || 
            phases.equals(Enums.of(FlushParticipantProtocol.Phase.STABILIZE, FlushParticipantProtocol.Phase.PROCESS)))
            coordinatorPhase = Phase.STABILIZE;
        else if (phases.equals(Enums.of(FlushParticipantProtocol.Phase.PROCESS, FlushParticipantProtocol.Phase.READY)))
            coordinatorPhase = Phase.PROCESS;
        else
            Assert.error();

        if (coordinatorPhase == Phase.READY)
        {
            installingMembership = null;
            installingMembershipDelta = null;
        }
        else
        {
            if (membershipManager.getMembership() != null)
            {
                Assert.notNull(installingMembershipDelta);
                Assert.isNull(installingMembership);
                
                installingMembership = Memberships.createMembership(membershipManager.getMembership(), installingMembershipDelta).newMembership;
            }
            else
            {
                Assert.isNull(installingMembershipDelta);
                Assert.notNull(installingMembership);
            }
        }
        
        this.installingMembership = installingMembership;
        this.installingMembershipDelta = installingMembershipDelta;
        phase = coordinatorPhase;
        flushStateResponses.clear();
        phaseRestartRequired = false;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.flushStateDetected(installingMembership != null ? 
                Strings.wrap(installingMembership.toString(), 4, 120) : "(not set)", 
                coordinatorPhase));
        
        proceed();
    }
    
    private void proceed()
    {
        if (stopped)
            return;
        
        if (phaseRestartRequired)
        {
            switch (phase)
            {
            case STABILIZE:
                phase = Phase.START;
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.flushPhaseRestarted());
                break;
            }
        }
        
        if (!flushProcessingRequired && phase == Phase.STABILIZE)
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.skipProcessingPhase());
            
            phase = Phase.PROCESS;
        }

        switch (phase)
        {
        case UNKNOWN:
            flushProcessingRequired = false;
            processingMembers.clear();
            respondingMembers = getRespondingMembers(null);
            startWaitTime = timeService.getCurrentTime();

            FlushMessagePart requestState = new FlushMessagePart(FlushMessagePart.Type.REQUEST_STATE, 
                getNodesIds(failureDetector.getFailedMembers()), getNodesIds(failureDetector.getLeftMembers()));
            
            for (IAddress node : respondingMembers)
                send(messageFactory.create(node, requestState, MessageFlags.HIGH_PRIORITY)); 
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.requestFlushState(Strings.wrap(respondingMembers.toString(), 4, 120)));
            break;
        case START:
            flushProcessingRequired = false;
            processingMembers.clear();
            respondingMembers = getRespondingMembers(null);
            startWaitTime = timeService.getCurrentTime();

            FlushStartMessagePart flushStartForNew = new FlushStartMessagePart(installingMembershipDelta == null, installingMembership, null,
                getNodesIds(failureDetector.getFailedMembers()), getNodesIds(failureDetector.getLeftMembers()));
            
            FlushStartMessagePart flushStartForOld = null;
            if (installingMembershipDelta != null)
                flushStartForOld = new FlushStartMessagePart(false, null, installingMembershipDelta, 
                    flushStartForNew.getFailedMembers(), flushStartForNew.getLeftMembers());
            
            for (IAddress node : respondingMembers)
            {
                boolean joined = false;
                if (installingMembershipDelta != null)
                {
                    for (INode joinedMember : installingMembershipDelta.getJoinedMembers())
                    {
                        if (joinedMember.getAddress().equals(node))
                        {
                            joined = true;
                            break;
                        }
                    }
                }
                else
                    joined = true;
                
                if (joined)
                    send(messageFactory.create(node, flushStartForNew, MessageFlags.HIGH_PRIORITY));
                else
                    send(messageFactory.create(node, flushStartForOld, MessageFlags.HIGH_PRIORITY));
            }
            
            phase = Phase.STABILIZE;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.startFlush(Strings.wrap(installingMembership.toString(), 4, 120), 
                    Strings.wrap(respondingMembers.toString(), 4, 120)));
            break;
        case STABILIZE:
            respondingMembers = getRespondingMembers(processingMembers);
            
            if (respondingMembers.isEmpty())
            {
                flushProcessingRequired = false;
                proceed();
                return;
            }
            
            startWaitTime = timeService.getCurrentTime();

            FlushMessagePart flushProcess = new FlushMessagePart(FlushMessagePart.Type.PROCESS, getNodesIds(failureDetector.getFailedMembers()),
                getNodesIds(failureDetector.getLeftMembers()));
            
            for (IAddress member : respondingMembers)
                send(messageFactory.create(member, flushProcess, MessageFlags.HIGH_PRIORITY));
            
            phase = Phase.PROCESS;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.processFlush(Strings.wrap(respondingMembers.toString(), 4, 120)));
            break;
        case PROCESS:
            respondingMembers = getRespondingMembers(null);
            startWaitTime = timeService.getCurrentTime();

            FlushMessagePart endFlush = new FlushMessagePart(FlushMessagePart.Type.END, getNodesIds(failureDetector.getFailedMembers()),
                getNodesIds(failureDetector.getLeftMembers()));
            
            for (IAddress member : respondingMembers)
                send(messageFactory.create(member, endFlush, MessageFlags.HIGH_PRIORITY));
            
            phase = Phase.END;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.endFlush(Strings.wrap(respondingMembers.toString(), 4, 120)));
            break;
        case END:
            phase = Phase.READY;
            installingMembership = null;
            installingMembershipDelta = null;
            flushProcessingRequired = false;
            processingMembers.clear();
            startWaitTime = 0;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.flushCompleted());
            break;
        }

        phaseRestartRequired = false;
    }

    private Set<UUID> getNodesIds(Set<INode> nodes)
    {
        Set<UUID> ids = new HashSet<UUID>(nodes.size());
        
        for (INode node : nodes)
            ids.add(node.getId());
        
        return ids;
    }
    
    private Set<UUID> getNodeIdsByAddress(Set<IAddress> nodes)
    {
        Set<UUID> ids = new HashSet<UUID>(nodes.size());
        
        for (INode node : installingMembership.getGroup().getMembers())
        {
            if (nodes.contains(node.getAddress()))
                ids.add(node.getId());
        }
        
        return ids;
    }

    private Set<IAddress> getRespondingMembers(Set<IAddress> members)
    {
        Set<INode> failedMembers = failureDetector.getFailedMembers();
        Set<INode> leftMembers = failureDetector.getLeftMembers();
        
        Set<IAddress> respondingMembers = new HashSet<IAddress>();
        for (INode member : installingMembership.getGroup().getMembers())
        {
            if ((members == null || members.contains(member.getAddress())) && !failedMembers.contains(member) && 
                !leftMembers.contains(member))
                respondingMembers.add(member.getAddress());
        }
        
        return respondingMembers;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Flush has been started.\nMembership:\n{0}\nParticipants:\n{1}.")
        ILocalizedMessage startFlush(String membership, String participants);
        @DefaultMessage("Response received from {0}. Not responded members:\n{1}.")
        ILocalizedMessage responseReceived(IAddress source, String respondingMembers);
        @DefaultMessage("Member {0} failed. Not responded members:\n{1}.")
        ILocalizedMessage memberFailed(IAddress member, String respondingMembers);
        @DefaultMessage("Member {0} left. Not responded members:\n{1}.")
        ILocalizedMessage memberLeft(IAddress member, String respondingMembers);
        @DefaultMessage("Processing phase of flush has been started on participants:\n{0}.")
        ILocalizedMessage processFlush(String participants);
        @DefaultMessage("End phase of flush has been started on participants:\n{0}.")
        ILocalizedMessage endFlush(String participants);
        @DefaultMessage("Requesting current flush state on participants:\n{0}.")
        ILocalizedMessage requestFlushState(String participants);
        @DefaultMessage("Flush completed.")
        ILocalizedMessage flushCompleted();
        @DefaultMessage("Current flush phase has been restarted.")
        ILocalizedMessage flushPhaseRestarted();
        @DefaultMessage("Processing phase has been skipped.")
        ILocalizedMessage skipProcessingPhase();
        @DefaultMessage("Flush coordinator has been changed to local node.")
        ILocalizedMessage flushCoordinatorChanged();
        @DefaultMessage("Flush state detected.\nMembership:\n{0}\nCoordinator phase: {1}.")
        ILocalizedMessage flushStateDetected(String installingMembership, Phase coordinatorPhase);
        @DefaultMessage("Membership installation has been started.\nMembership:\n{0}.")
        ILocalizedMessage membershipInstalling(String installingMembership);
    }
}
