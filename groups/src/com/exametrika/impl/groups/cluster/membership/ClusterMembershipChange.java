/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.groups.cluster.IClusterMembershipChange;
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
    private final List<IDomainMembershipChange> domains;
    private final Map<String, IDomainMembershipChange> domainsMap;

    public ClusterMembershipChange(List<IDomainMembershipChange> domains)
    {
        Assert.notNull(domains);

        this.domains = Immutables.wrap(domains);
        
        Map<String, IDomainMembershipChange> domainsMap = new HashMap<String, IDomainMembershipChange>();
        for (IDomainMembershipChange domain : domains)
            domainsMap.put(domain.getName(), domain);
        
        this.domainsMap = domainsMap;
    }

    @Override
    public IDomainMembershipChange findDomain(String name)
    {
        Assert.notNull(name);
        
        return domainsMap.get(name);
    }
    
    @Override
    public List<IDomainMembershipChange> getDomains()
    {
        return domains;
    }

    @Override
    public String toString()
    {
        return Strings.toString(domains, false);
    }
}
