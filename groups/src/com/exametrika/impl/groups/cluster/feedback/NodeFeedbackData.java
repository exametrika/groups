/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;

/**
 * The {@link NodeFeedbackData} is a node feedback data.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeFeedbackData implements IExchangeData
{
    private final List<INodeState> states;

    public NodeFeedbackData(List<INodeState> states)
    {
        Assert.notNull(states);
        
        this.states = Immutables.wrap(states);
    }
    
    public List<INodeState> getStates()
    {
        return states;
    }

    @Override
    public long getId()
    {
        return 0;
    }

    @Override
    public int getSize()
    {
        return 65536;
    }
}
