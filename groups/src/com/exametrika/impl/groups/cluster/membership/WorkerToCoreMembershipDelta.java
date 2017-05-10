/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link WorkerToCoreMembershipDelta} is worker to core node mapping membership delta.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipDelta implements IClusterMembershipElementDelta
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<INode> joinedCoreNodes;
    private final Set<UUID> leftCoreNodes;
    private final Set<UUID> failedCoreNodes;
    private final Map<UUID, UUID> newCoreByWorkerMap;

    public WorkerToCoreMembershipDelta(List<INode> joinedCoreNodes, Set<UUID> leftCoreNodes, Set<UUID> failedCoreNodes,
        Map<UUID, UUID> newCoreByWorkerMap)
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
    
    public Set<UUID> getLeftCoreNodes()
    {
        return leftCoreNodes;
    }
    
    public Set<UUID> getFailedCoreNodes()
    {
        return failedCoreNodes;
    }
    
    public Map<UUID, UUID> getNewCoreByWorkerMap()
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
        @DefaultMessage("joined core nodes: {0}\nleft core nodes: {1}\nfailed core nodes: {2}\nnew worker to core node mappings: {3}")
        ILocalizedMessage toString(List<INode> joinedMembers, Set<UUID> leftMembers, Set<UUID> failedMembers,
            Map<UUID, UUID> newCoreByWorkerMap);
    }
}
