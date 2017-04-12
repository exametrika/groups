/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.List;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IWorkerControllerObserver;

/**
 * The {@link WorkerFeedbackProtocol} represents a worker feedback data exchange protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerFeedbackProtocol extends AbstractFeedbackProtocol implements IWorkerControllerObserver
{
    private IAddress controller;
    
    public WorkerFeedbackProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager, 
        List<IFeedbackProvider> feedbackProviders, List<IFeedbackListener> listeners, long dataExchangePeriod)
    {
        super(channelName, messageFactory, membershipManager, feedbackProviders, listeners, dataExchangePeriod);
    }

    @Override
    public void onControllerChanged(IAddress controller)
    {
        this.controller = controller;
        
        sendData(true);
    }
    
    @Override
    protected IAddress getDestination()
    {
        return controller;
    }
}
