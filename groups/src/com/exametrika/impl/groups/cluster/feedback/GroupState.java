/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link GroupState} is a group state.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupState implements IGroupState
{
    private final String domain;
    private final UUID id;
    private final long membershipId;
    private final List<UUID> members;
    private final boolean primary;
    private final State state;

    public GroupState(String domain, UUID id, long membershipId, List<UUID> members, boolean primary, State state)
    {
        Assert.notNull(domain);
        Assert.notNull(id);
        Assert.notNull(members);
        Assert.notNull(state);
        
        this.domain = domain;
        this.id = id;
        this.membershipId = membershipId;
        this.members = Immutables.wrap(members);
        this.primary = primary;
        this.state = state;
    }
    
    @Override
    public String getDomain()
    {
        return domain;
    }
    
    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public long getMembershipId()
    {
        return membershipId;
    }

    @Override
    public List<UUID> getMembers()
    {
        return members;
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    @Override
    public State getState()
    {
        return state;
    }
}
