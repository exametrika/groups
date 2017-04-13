/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.feedback.IGroupState.State;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;

/**
 * The {@link WorkerGroupStateUpdater} is an updater of worker group state.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupStateUpdater implements IFlushParticipant
{
    private final IGroupFeedbackService feedbackService;
    private boolean coordinator;
    private IFlush flush;
    
    public WorkerGroupStateUpdater(IGroupFeedbackService feedbackService)
    {
        Assert.notNull(feedbackService);
        
        this.feedbackService = feedbackService;
    }
    
    @Override
    public boolean isFlushProcessingRequired()
    {
        return false;
    }

    @Override
    public void setCoordinator()
    {
        coordinator = true;
    }

    @Override
    public void startFlush(IFlush flush)
    {
        if (this.flush == null && coordinator)
        {
            IGroupState state = buildGroupState(flush, true);
            feedbackService.updateGroupState(state);
        }
        
        this.flush = flush;
        flush.grantFlush(this);
    }

    @Override
    public void beforeProcessFlush()
    {
    }

    @Override
    public void processFlush()
    {
        flush.grantFlush(this);
    }

    @Override
    public void endFlush()
    {
        if (flush != null)
        {
            IGroupState state = buildGroupState(flush, false);
            feedbackService.updateGroupState(state);
            flush = null;
        }
    }

    private IGroupState buildGroupState(IFlush flush, boolean startFlush)
    {
        IGroupMembership membership = flush.getNewMembership();
        IGroup group = membership.getGroup();
        
        List<UUID> members = new ArrayList<UUID>();
        for (INode member : group.getMembers())
            members.add(member.getId());
        
        return new GroupState(group.getCoordinator().getDomain(), group.getId(), membership.getId(), members, 
            group.isPrimary(), startFlush ? State.FLUSH : State.NORMAL);
    }
}
