/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.utils.Assert;

/**
 * The {@link WorkerNodeTrackingStrategy} represents a worker controller node tracking strategy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerNodeTrackingStrategy implements INodeTrackingStrategy
{
    private final WorkerFailureDetectionProtocol failureDetectionProtocol;
    
    public WorkerNodeTrackingStrategy(WorkerFailureDetectionProtocol failureDetectionProtocol)
    {
        Assert.notNull(failureDetectionProtocol);
        
        this.failureDetectionProtocol = failureDetectionProtocol;
    }
    
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        IAddress controller = failureDetectionProtocol.getController();
        if (controller != null)
            return Collections.singleton(controller);
        else
            return Collections.emptySet();
    }
}
