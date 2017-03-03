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
 * The {@link DomainMembershipDelta} is implementation of {@link IDomainMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DomainMembershipDelta implements IDomainMembershipDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final List<IClusterMembershipElementDelta> deltas;

    public DomainMembershipDelta(String name, List<IClusterMembershipElementDelta> deltas)
    {
        Assert.notNull(name);
        Assert.notNull(deltas);

        this.name = name;
        this.deltas = Immutables.wrap(deltas);
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public List<IClusterMembershipElementDelta> getDeltas()
    {
        return deltas;
    }

    @Override
    public String toString()
    {
        return messages.toString(name, Strings.toString(deltas, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("name : {0}, deltas: \n{1}")
        ILocalizedMessage toString(String name, String deltas);
    }
}
