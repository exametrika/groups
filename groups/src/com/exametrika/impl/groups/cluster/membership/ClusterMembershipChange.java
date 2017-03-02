/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembershipChange;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
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
    private final List<IClusterMembershipElementChange> changes;

    public ClusterMembershipChange(List<IClusterMembershipElementChange> changes)
    {
        Assert.notNull(changes);

        this.changes = Immutables.wrap(changes);
    }

    @Override
    public <T extends IClusterMembershipElementChange> T getChange(Class<T> changeClass)
    {
        Assert.notNull(changeClass);
        
        for (IClusterMembershipElementChange change : changes)
        {
            if (changeClass.isInstance(change))
                return (T)change;
        }
        
        return Assert.error();
    }
    
    @Override
    public List<IClusterMembershipElementChange> getChanges()
    {
        return changes;
    }

    @Override
    public String toString()
    {
        return Strings.toString(changes, false);
    }
}
