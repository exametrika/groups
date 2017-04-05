/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link NodesMembershipDelta} is implementation of {@link IClusterMembershipElementDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodesMembershipDelta implements IClusterMembershipElementDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<INode> joinedNodes;
    private final Set<UUID> leftNodes;
    private final Set<UUID> failedNodes;

    public NodesMembershipDelta(List<INode> joinedNodes, Set<UUID> leftNodes, Set<UUID> failedNodes)
    {
        Assert.notNull(joinedNodes);
        Assert.notNull(leftNodes);
        Assert.notNull(failedNodes);
        
        this.joinedNodes = Immutables.wrap(joinedNodes);
        this.leftNodes = Immutables.wrap(leftNodes);
        this.failedNodes = Immutables.wrap(failedNodes);
    }

    public List<INode> getJoinedNodes()
    {
        return joinedNodes;
    }
    
    public Set<UUID> getLeftNodes()
    {
        return leftNodes;
    }
    
    public Set<UUID> getFailedNodes()
    {
        return failedNodes;
    }

    @Override
    public String toString()
    {
        return messages.toString(joinedNodes, leftNodes, failedNodes).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("joined: {0}\nleft: {1}\nfailed: {2}")
        ILocalizedMessage toString(List<INode> joinedNodes, Set<UUID> leftNodes, Set<UUID> failedNodes);
    }
}
