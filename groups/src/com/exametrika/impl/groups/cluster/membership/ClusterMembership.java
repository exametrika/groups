/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ClusterMembership} is implementation of {@link IClusterMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembership implements IClusterMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final List<IDomainMembership> domains;
    private final Map<String, IDomainMembership> domainsMap;

    public ClusterMembership(long id, List<IDomainMembership> domains)
    {
        Assert.isTrue(id > 0);
        Assert.notNull(domains);

        this.id = id;
        this.domains = Immutables.wrap(domains);
        
        Map<String, IDomainMembership> domainsMap = new HashMap<String, IDomainMembership>();
        for (IDomainMembership domain : domains)
            domainsMap.put(domain.getName(), domain);
        
        this.domainsMap = domainsMap;
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public IDomainMembership findDomain(String name)
    {
        Assert.notNull(name);
        
        return domainsMap.get(name);
    }
    
    @Override
    public List<IDomainMembership> getDomains()
    {
        return domains;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof ClusterMembership))
            return false;

        ClusterMembership membership = (ClusterMembership)o;
        return id == membership.id;
    }

    @Override
    public int hashCode()
    {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public String toString()
    {
        return messages.toString(id, Strings.toString(domains, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id : {0}, domains: \n{1}")
        ILocalizedMessage toString(long id, String elements);
    }
}
