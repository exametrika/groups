/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link MembershipResponseMessagePart} is a group membership response message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipResponseMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IAddress> healthyMembers;
    private final long membershipId;

    public MembershipResponseMessagePart(long membershipId, List<IAddress> healthyMembers)
    {
        Assert.notNull(healthyMembers);
        Assert.isTrue(!healthyMembers.isEmpty());
        
        this.membershipId = membershipId;
        this.healthyMembers = Immutables.wrap(healthyMembers);
    }

    public long getMembershipId()
    {
        return membershipId;
    }
    
    public List<IAddress> getHealthyMembers()
    {
        return healthyMembers;
    }
    
    @Override
    public int getSize()
    {
        return healthyMembers.size() * 100;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(healthyMembers).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("healthy members: {0}")
        ILocalizedMessage toString(List<IAddress> healthyMembers);
    }
}

