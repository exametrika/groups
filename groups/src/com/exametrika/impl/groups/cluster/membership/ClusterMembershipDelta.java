/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ClusterMembershipDelta} is implementation of {@link IClusterMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipDelta implements IClusterMembershipDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final boolean full;
    private final List<IDomainMembershipDelta> domains;

    public ClusterMembershipDelta(long id, boolean full, List<IDomainMembershipDelta> domains)
    {
        Assert.isTrue(id > 0);
        Assert.notNull(domains);

        this.id = id;
        this.full = full;
        this.domains = Immutables.wrap(domains);
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public boolean isFull()
    {
        return full;
    }
    
    @Override
    public List<IDomainMembershipDelta> getDomains()
    {
        return domains;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof ClusterMembershipDelta))
            return false;

        ClusterMembershipDelta delta = (ClusterMembershipDelta)o;
        return id == delta.id;
    }

    @Override
    public int hashCode()
    {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public String toString()
    {
        return messages.toString(id, full, Strings.toString(domains, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id : {0}, full: {1}, domains: \n{2}")
        ILocalizedMessage toString(long id, boolean full, String domains);
    }
}
