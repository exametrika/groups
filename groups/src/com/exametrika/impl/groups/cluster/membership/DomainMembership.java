/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link DomainMembership} is implementation of {@link IDomainMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DomainMembership implements IDomainMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final List<IClusterMembershipElement> elements;

    public DomainMembership(String name, List<IClusterMembershipElement> elements)
    {
        Assert.notNull(name);
        Assert.notNull(elements);

        this.name = name;
        this.elements = Immutables.wrap(elements);
    }

    @Override
    public String getName()
    {
        return name;
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
    public String toString()
    {
        return messages.toString(name, Strings.toString(elements, true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("name : {0}, elements: \n{1}")
        ILocalizedMessage toString(String name, String elements);
    }
}
