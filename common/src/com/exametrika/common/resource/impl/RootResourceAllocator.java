/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link RootResourceAllocator} is a top level implementation of {@link IResourceAllocator} which allocates full resource
 * to its consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RootResourceAllocator extends ResourceAllocator implements ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RootResourceAllocator.class);
    protected final IResourceProvider resourceProvider;
    protected final long allocationPeriod;
    private final Timer timer;
    private long lastAllocationTime;

    public RootResourceAllocator(String name, IResourceProvider resourceProvider, Map<String, IAllocationPolicy> policies, 
        IAllocationPolicy defaultPolicy, long timerPeriod, long allocationPeriod, long quotaIncreaseDelay, long initializePeriod,
        ITimeService timeService)
    {
        super(name, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod, timeService);
        
        Assert.notNull(resourceProvider);

        this.resourceProvider = resourceProvider;
        this.allocationPeriod = allocationPeriod;
        timer = new Timer(timerPeriod, this, false, "[" + name + "] resource allocator timer thread", null);
    }
    
    @Override
    public void start()
    {
        if (initializePeriod > 0)
            startTime = timeService.getCurrentTime();
        else
            initialized = true;
        
        allocate();
        
        timer.start();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());
    }

    @Override
    public void stop()
    {
        timer.stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public void onTimer()
    {
        long currentTime = timeService.getCurrentTime();
        
        if (lastAllocationTime == 0 || currentTime > lastAllocationTime + allocationPeriod)
        {
            allocate();
            lastAllocationTime = currentTime;
        }
        
        super.onTimer();
    }
    
    protected void allocate()
    {
        setQuota(resourceProvider.getAmount());
    }
    
    private interface IMessages
    {
        @DefaultMessage("Resource allocator started.")
        ILocalizedMessage started();
        
        @DefaultMessage("Resource allocator stopped.")
        ILocalizedMessage stopped();
    }
}
