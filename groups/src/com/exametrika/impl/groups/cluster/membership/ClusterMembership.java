/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
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
    private final List<IClusterMembershipElement> elements;

    public ClusterMembership(long id, List<IClusterMembershipElement> elements)
    {
        Assert.isTrue(id > 0);
        Assert.notNull(elements);

        this.id = id;
        this.elements = Immutables.wrap(elements);
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public <T extends IClusterMembershipElement> T getElement(Class<T> elementClass)
    {
        Assert.notNull(elementClass);
        
        for (IClusterMembershipElement element : elements)
        {
            if (elementClass.isInstance(element))
                return (T)element;
        }
        
        return Assert.error();
    }
    
    @Override
    public List<IClusterMembershipElement> getElements()
    {
        return elements;
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
        return messages.toString(id, Strings.toString(elements, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id : {0}, elements: \n{1}")
        ILocalizedMessage toString(long id, String elements);
    }
}
