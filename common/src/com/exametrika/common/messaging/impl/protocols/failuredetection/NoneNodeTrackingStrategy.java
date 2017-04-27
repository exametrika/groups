/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link NoneNodeTrackingStrategy} is a none tracking strategy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class NoneNodeTrackingStrategy implements INodeTrackingStrategy
{
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        return Collections.emptySet();
    }
}
