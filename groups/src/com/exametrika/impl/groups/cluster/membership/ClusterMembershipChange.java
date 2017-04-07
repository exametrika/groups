/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ClusterMembershipChange} is implementation of {@link IClusterMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipChange implements IClusterMembershipChange
{
    private final List<IDomainMembership> newDomains;
    private final List<IDomainMembershipChange> changedDomains;
    private final Map<String, IDomainMembershipChange> changedDomainsMap;
    private final Set<IDomainMembership> removedDomains;

    public ClusterMembershipChange(List<IDomainMembership> newDomains, List<IDomainMembershipChange> changedDomains,
        Set<IDomainMembership> removedDomains)
    {
        Assert.notNull(newDomains);
        Assert.notNull(changedDomains);
        Assert.notNull(removedDomains);

        this.newDomains = Immutables.wrap(newDomains);
        this.changedDomains = Immutables.wrap(changedDomains);
        this.removedDomains = Immutables.wrap(removedDomains);
        
        Map<String, IDomainMembershipChange> changedDomainsMap = new HashMap<String, IDomainMembershipChange>();
        for (IDomainMembershipChange domain : changedDomains)
            changedDomainsMap.put(domain.getName(), domain);
        
        this.changedDomainsMap = changedDomainsMap;
    }

    @Override
    public List<IDomainMembership> getNewDomains()
    {
        return newDomains;
    }

    @Override
    public IDomainMembershipChange findChangedDomain(String name)
    {
        Assert.notNull(name);
        
        return changedDomainsMap.get(name);
    }
    
    @Override
    public List<IDomainMembershipChange> getChangedDomains()
    {
        return changedDomains;
    }

    @Override
    public Set<IDomainMembership> getRemovedDomains()
    {
        return removedDomains;
    }
    
    @Override
    public String toString()
    {
        return Strings.toString(changedDomains, false);
    }
}
