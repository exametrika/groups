/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MembershipEvent} is a group membership event.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipEvent
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMembership oldMembership;
    private final IMembership newMembership;
    private final IMembershipChange membershipChange;

    /**
     * Creates a new object.
     *
     * @param oldMembership old membership
     * @param newMembership new membership
     * @param membershipChange membership change 
     */
    public MembershipEvent(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
    {
        Assert.notNull(oldMembership);
        Assert.notNull(newMembership);
        Assert.notNull(membershipChange);

        this.oldMembership = oldMembership;
        this.newMembership = newMembership;
        this.membershipChange = membershipChange;
    }

    /**
     * Returns a old group membership.
     * 
     * @return old group membership
     */
    public IMembership getOldMembership()
    {
        return oldMembership;
    }
    
    /**
     * Returns a new group membership.
     * 
     * @return new group membership
     */
    public IMembership getNewMembership()
    {
        return newMembership;
    }

    /**
     * Returns membership change.
     * 
     * @return membership change
     */
    public IMembershipChange getMembershipChange()
    {
        return membershipChange;
    }
    
    @Override
    public String toString()
    {
        return messages.toString(oldMembership, newMembership, membershipChange).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("old: {0}\nnew: {1}\nchange: {2}")
        ILocalizedMessage toString(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange);
    }
}