/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.Set;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link DiscoveryMessagePart} is a discovery message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DiscoveryMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<INode> discoveredNodes;

    public DiscoveryMessagePart(Set<INode> discoveredNodes)
    {
        Assert.notNull(discoveredNodes);
        
        this.discoveredNodes = Immutables.wrap(discoveredNodes);
    }
    
    public Set<INode> getDiscoveredNodes()
    {
        return discoveredNodes;
    }
    
    @Override
    public int getSize()
    {
        return discoveredNodes.size() * 1000;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(discoveredNodes).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("discovered nodes: {0}")
        ILocalizedMessage toString(Set<INode> discoveredNodes);
    }
}

