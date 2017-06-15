/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.utils.Assert;

/**
 * The {@link WorkerControllerNodeTrackingStrategy} represents a worker node tracking strategy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerControllerNodeTrackingStrategy implements INodeTrackingStrategy
{
    private final CoreClusterFailureDetectionProtocol failureDetectionProtocol;
    
    public WorkerControllerNodeTrackingStrategy(CoreClusterFailureDetectionProtocol failureDetectionProtocol)
    {
        Assert.notNull(failureDetectionProtocol);
        
        this.failureDetectionProtocol = failureDetectionProtocol;
    }
    
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        return failureDetectionProtocol.getWorkerNodes();
    }
}
