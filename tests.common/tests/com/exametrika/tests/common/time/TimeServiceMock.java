/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.time;

import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Times;

/**
 * The {@link TimeServiceMock} is a mock implementation of {@link ITimeService}.
 * 
 * @see ITimeService
 * @author medvedev
 */
public class TimeServiceMock implements ITimeService
{
    public volatile boolean useSystemTime = true;
    public volatile long time;
    
    @Override
    public long getCurrentTime()
    {
        if (useSystemTime)
            return Times.getCurrentTime();
        
        return time;
    }
}
