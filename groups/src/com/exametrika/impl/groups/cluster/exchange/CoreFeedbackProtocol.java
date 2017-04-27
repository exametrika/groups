/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;

/**
 * The {@link CoreFeedbackProtocol} represents a core feedback data exchange protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreFeedbackProtocol extends AbstractFeedbackProtocol implements IPreparedGroupMembershipListener,
    IFailureDetectionListener
{
    private final IGroupMembershipService membershipService;
    private final IGroupFailureDetector failureDetector;
    private IAddress coordinator;
    private ISender bridgeSender;
    
    public CoreFeedbackProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager, 
        List<IFeedbackProvider> feedbackProviders, List<IFeedbackListener> listeners, long dataExchangePeriod, 
        IGroupFailureDetector failureDetector, IGroupMembershipService membershipService)
    {
        super(channelName, messageFactory, membershipManager, feedbackProviders, listeners, dataExchangePeriod);
        
        Assert.notNull(failureDetector);
        Assert.notNull(membershipService);
        
        this.failureDetector = failureDetector;
        this.membershipService = membershipService;
    }

    public void setBridgeSender(ISender bridgeSender)
    {
        Assert.notNull(bridgeSender);
        Assert.isNull(this.bridgeSender);
        
        this.bridgeSender = bridgeSender;
    }
    
    @Override
    protected IAddress getDestination()
    {
        return coordinator;
    }

    @Override
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership,
        IGroupMembershipChange change)
    {
        updateCoordinator();
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        updateCoordinator();
    }

    @Override
    public void onMemberLeft(INode member)
    {
        updateCoordinator();
    }
    
    @Override
    protected ISender getFeedbackSender()
    {
        if (bridgeSender != null)
            return bridgeSender;
        else
            return getSender();
    }
    
    private void updateCoordinator()
    {
        INode currentCoordinator = failureDetector.getCurrentCoordinator();
        if (currentCoordinator != null && !membershipService.getLocalNode().equals(currentCoordinator))
        {
            if (!currentCoordinator.getAddress().equals(coordinator))
            {
                coordinator = currentCoordinator.getAddress();
                sendData(true);
            }
        }
        else
            coordinator = null;
    }
}
