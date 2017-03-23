/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link DomainMembershipChange} is implementation of {@link IDomainMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DomainMembershipChange implements IDomainMembershipChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final List<IClusterMembershipElementChange> changes;

    public DomainMembershipChange(String name, List<IClusterMembershipElementChange> changes)
    {
        Assert.notNull(name);
        Assert.notNull(changes);

        this.name = name;
        this.changes = Immutables.wrap(changes);
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public <T extends IClusterMembershipElementChange> T findChange(Class<T> changeClass)
    {
        Assert.notNull(changeClass);
        
        for (IClusterMembershipElementChange change : changes)
        {
            if (changeClass.isInstance(change))
                return (T)change;
        }
        
        return null;
    }
    
    @Override
    public List<IClusterMembershipElementChange> getChanges()
    {
        return changes;
    }

    @Override
    public String toString()
    {
        return messages.toString(name, Strings.toString(changes, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("name : {0}, changes: \n{1}")
        ILocalizedMessage toString(String name, String changes);
    }
}
