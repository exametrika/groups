/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link WorkerToCoreMembershipChange} is worker to core node mapping membership change.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipChange implements IClusterMembershipElementChange
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<INode> joinedCoreNodes;
    private final Set<INode> leftCoreNodes;
    private final Set<INode> failedCoreNodes;
    private final Map<INode, INode> newCoreByWorkerMap;

    public WorkerToCoreMembershipChange(List<INode> joinedCoreNodes, Set<INode> leftCoreNodes, Set<INode> failedCoreNodes, 
        Map<INode, INode> newCoreByWorkerMap)
    {
        Assert.notNull(joinedCoreNodes);
        Assert.notNull(leftCoreNodes);
        Assert.notNull(failedCoreNodes);
        Assert.notNull(newCoreByWorkerMap);

        this.joinedCoreNodes = Immutables.wrap(joinedCoreNodes);
        this.leftCoreNodes = Immutables.wrap(leftCoreNodes);
        this.failedCoreNodes = Immutables.wrap(failedCoreNodes);
        this.newCoreByWorkerMap = Immutables.wrap(newCoreByWorkerMap);
    }

    public List<INode> getJoinedCoreNodes()
    {
        return joinedCoreNodes;
    }
    
    public Set<INode> getLeftCoreNodes()
    {
        return leftCoreNodes;
    }
    
    public Set<INode> getFailedCoreNodes()
    {
        return failedCoreNodes;
    }
    
    public Map<INode, INode> getNewCoreByWorkerMap()
    {
        return newCoreByWorkerMap;
    }

    @Override
    public String toString()
    {
        return messages.toString(joinedCoreNodes, leftCoreNodes, failedCoreNodes, newCoreByWorkerMap).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("joined core nodes: {0}\nleft core nodes: {1}\nfailed core nodes: {2}, new worker to core node mappings: {3}")
        ILocalizedMessage toString(List<INode> joinedMembers, Set<INode> leftMembers, Set<INode> failedMembers,
            Map<INode, INode> newCoreByWorkerMap);
    }
}
