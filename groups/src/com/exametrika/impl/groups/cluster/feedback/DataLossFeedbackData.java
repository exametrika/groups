/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;

/**
 * The {@link DataLossFeedbackData} is a data loss feedback data.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DataLossFeedbackData implements IExchangeData
{
    private final List<IDataLossState> states;

    public DataLossFeedbackData(List<IDataLossState> states)
    {
        Assert.notNull(states);
        
        this.states = Immutables.wrap(states);
    }
    
    public List<IDataLossState> getStates()
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
