/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.Set;

import com.exametrika.api.groups.cluster.INode;
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
    private final boolean core;

    public DiscoveryMessagePart(Set<INode> discoveredNodes, boolean core)
    {
        Assert.notNull(discoveredNodes);
        
        this.discoveredNodes = Immutables.wrap(discoveredNodes);
        this.core = core;
    }
    
    public boolean isCore()
    {
        return core;
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
        return messages.toString(discoveredNodes, core).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("discovered nodes: {0}, core: {1}")
        ILocalizedMessage toString(Set<INode> discoveredNodes, boolean core);
    }
}

