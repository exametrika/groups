/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;

/**
 * The {@link GroupFeedbackData} is a group feedback data.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupFeedbackData implements IExchangeData
{
    private final List<IGroupState> states;

    public GroupFeedbackData(List<IGroupState> states)
    {
        Assert.notNull(states);
        
        this.states = Immutables.wrap(states);
    }
    
    public List<IGroupState> getStates()
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
