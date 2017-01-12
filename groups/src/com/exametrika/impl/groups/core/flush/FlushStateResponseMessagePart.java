/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.membership.IMembershipDelta;

/**
 * The {@link FlushStateResponseMessagePart} is a flush state response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushStateResponseMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final FlushParticipantProtocol.Phase phase; 
    private final IMembership preparedMembership;
    private final IMembershipDelta preparedMembershipDelta;
    private final boolean flushProcessingRequired;
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;
    private final List<Object> coordinatorStates;

    public FlushStateResponseMessagePart(FlushParticipantProtocol.Phase phase, IMembership preparedMembership, 
        IMembershipDelta preparedMembershipDelta, boolean flushProcessingRequired, Set<UUID> failedMembers,
        Set<UUID> leftMembers, List<Object> coordinatorStates)
    {
        Assert.notNull(phase);
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(coordinatorStates);
        
        this.phase = phase;
        this.preparedMembership = preparedMembership;
        this.preparedMembershipDelta = preparedMembershipDelta;
        this.flushProcessingRequired = flushProcessingRequired;
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.coordinatorStates = Immutables.wrap(coordinatorStates);
    }
    
    public FlushParticipantProtocol.Phase getPhase()
    {
        return phase;
    }
    
    public IMembership getPreparedMembership()
    {
        return preparedMembership;
    }
    
    public IMembershipDelta getPreparedMembershipDelta()
    {
        return preparedMembershipDelta;
    }
    
    public boolean isFlushProcessingRequired()
    {
        return flushProcessingRequired;
    }
    
    public Set<UUID> getFailedMembers()
    {
        return failedMembers;
    }
    
    public Set<UUID> getLeftMembers()
    {
        return leftMembers;
    }
    
    public List<Object> getCoordinatorStates()
    {
        return coordinatorStates;
    }
    
    @Override
    public int getSize()
    {
        return 50000;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(phase, (preparedMembership != null ? preparedMembership.toString() : messages.notSet().toString()), 
            (preparedMembershipDelta != null ? preparedMembershipDelta.toString() : messages.notSet().toString()), 
            flushProcessingRequired, failedMembers, leftMembers, coordinatorStates).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("phase: {0}, prepared membership: {1}, prepared membership delta: {2}, flush processing required: {3}, failed members: {4}, left members: {5}, coordinator states: {6}")
        ILocalizedMessage toString(FlushParticipantProtocol.Phase phase, String preparedMembership, 
            String preparedMembershipDelta, boolean flushProcessingRequired, Set<UUID> failedMembers, Set<UUID> leftMembers, 
            List<Object> coordinatorStates);
        @DefaultMessage("(not set)")
        ILocalizedMessage notSet();
    }
}