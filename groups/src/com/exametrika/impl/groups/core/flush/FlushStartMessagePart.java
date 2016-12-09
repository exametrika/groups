/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

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
 * The {@link FlushStartMessagePart} is a flush start message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class FlushStartMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean groupForming;
    private final IMembership preparedMembership;
    private final IMembershipDelta preparedMembershipDelta;
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;

    public FlushStartMessagePart(boolean groupForming, IMembership preparedMembership, IMembershipDelta preparedMembershipDelta, 
        Set<UUID> failedMembers, Set<UUID> leftMembers)
    {
        Assert.isTrue(preparedMembership != null || preparedMembershipDelta != null);
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        
        this.groupForming = groupForming;
        this.preparedMembership = preparedMembership;
        this.preparedMembershipDelta = preparedMembershipDelta;
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
    }
    
    public boolean isGroupForming()
    {
        return groupForming;
    }
    
    public IMembership getPreparedMembership()
    {
        return preparedMembership;
    }
    
    public IMembershipDelta getPreparedMembershipDelta()
    {
        return preparedMembershipDelta;
    }
    
    public Set<UUID> getFailedMembers()
    {
        return failedMembers;
    }
    
    public Set<UUID> getLeftMembers()
    {
        return leftMembers;
    }
    
    @Override
    public int getSize()
    {
        return 50000;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString((preparedMembership != null ? preparedMembership.toString() : messages.notSet().toString()), 
            (preparedMembershipDelta != null ? preparedMembershipDelta.toString() : messages.notSet().toString()), 
            failedMembers, leftMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("prepared membership: {0}, prepared membership delta: {1}, failed members: {2}, left members: {3}")
        ILocalizedMessage toString(String preparedMembership, String preparedMembershipDelta, 
            Set<UUID> failedMembers, Set<UUID> leftMembers);
        @DefaultMessage("(not set)")
        ILocalizedMessage notSet();
    }
}

