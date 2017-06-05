/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.impl.groups.cluster.feedback.IDataLossFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.IDataLossState;

public class DataLossFeedbackProviderMock implements IDataLossFeedbackService
{
    private IDataLossState state;

    public IDataLossState getState()
    {
        return state;
    }
    
    @Override
    public void updateDataLossState(IDataLossState state)
    {
        this.state = state;
    }
}
