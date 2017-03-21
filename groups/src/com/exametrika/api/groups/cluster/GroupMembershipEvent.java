/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupMembershipEvent} is a group membership event.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembershipEvent
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembership oldMembership;
    private final IGroupMembership newMembership;
    private final IGroupMembershipChange membershipChange;

    /**
     * Creates a new object.
     *
     * @param oldMembership old membership
     * @param newMembership new membership
     * @param membershipChange membership change 
     */
    public GroupMembershipEvent(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
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
    public IGroupMembership getOldMembership()
    {
        return oldMembership;
    }
    
    /**
     * Returns a new group membership.
     * 
     * @return new group membership
     */
    public IGroupMembership getNewMembership()
    {
        return newMembership;
    }

    /**
     * Returns membership change.
     * 
     * @return membership change
     */
    public IGroupMembershipChange getMembershipChange()
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
        ILocalizedMessage toString(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange);
    }
}