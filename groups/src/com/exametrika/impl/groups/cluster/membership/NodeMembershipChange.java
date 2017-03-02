/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;

import com.exametrika.api.groups.cluster.INodeMembershipChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link NodeMembershipChange} is implementation of {@link INodeMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeMembershipChange implements INodeMembershipChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<INode> joinedNodes;
    private final Set<INode> leftNodes;
    private final Set<INode> failedNodes;

    public NodeMembershipChange(Set<INode> joinedNodes, Set<INode> leftNodes, Set<INode> failedNodes)
    {
        Assert.notNull(joinedNodes);
        Assert.notNull(leftNodes);
        Assert.notNull(failedNodes);
        
        this.joinedNodes = Immutables.wrap(joinedNodes);
        this.leftNodes = Immutables.wrap(leftNodes);
        this.failedNodes = Immutables.wrap(failedNodes);
    }

    @Override
    public Set<INode> getJoinedNodes()
    {
        return joinedNodes;
    }
    
    @Override
    public Set<INode> getLeftNodes()
    {
        return leftNodes;
    }
    
    @Override
    public Set<INode> getFailedNodes()
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
        ILocalizedMessage toString(Set<INode> joinedNodes, Set<INode> leftNodes, Set<INode> failedNodes);
    }
}
