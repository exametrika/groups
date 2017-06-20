/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.List;
import java.util.Random;

import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link TestWorkerClusterFailureGenerationProtocol} is a worker cluster protocol which generates failures for local node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestWorkerClusterFailureGenerationProtocol extends AbstractProtocol
{
    private final List<TestFailureSpec> failureSpecs;
    private final IChannelReconnector channelReconnector;
    private final long processPeriod;
    private final Random random = new Random();
    private long lastProcessTime;
    private long[] nextFailureTimes;
   
    public TestWorkerClusterFailureGenerationProtocol(String channelName, IMessageFactory messageFactory, List<TestFailureSpec> failureSpecs,
        IChannelReconnector channelReconnector, long processPeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(failureSpecs);
        Assert.notNull(channelReconnector);
        
        this.failureSpecs = failureSpecs;
        this.processPeriod = processPeriod;
        this.channelReconnector = channelReconnector;
        this.nextFailureTimes = new long[failureSpecs.size()];
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (currentTime > lastProcessTime + processPeriod)
            processFail(currentTime);
    }

    private void processFail(long currentTime)
    {
        if (isFailed(currentTime))
            channelReconnector.reconnect();
      
        lastProcessTime = currentTime;
    }

    private boolean isFailed(long currentTime)
    {
        boolean failed = false;
        for (int i = 0; i < failureSpecs.size(); i++)
        {
            TestFailureSpec failureSpec = failureSpecs.get(i);
            
            switch (failureSpec.getFailureEventType())
            {
            case RANDOM:
                break;
            default:
                continue;
            }
            
            if (currentTime < nextFailureTimes[i])
                continue;
            
            switch(failureSpec.getFailureTarget())
            {
            case RANDOM_NODE:
            case RANDOM_WORKER_NODE:
                break;
            default:
                continue;
            }

            nextFailureTimes[i] = currentTime + getFailurePeriod(failureSpec);
        }
        
        return failed;
    }
    
    private long getFailurePeriod(TestFailureSpec failureSpec)
    {
        switch(failureSpec.getFailurePeriodType())
        {
        case SET:
            return failureSpec.getFailurePeriod();
        case RANDOM:
            return random.nextInt((int)failureSpec.getFailurePeriod());
        default:
            return Assert.error();
        }
    }
}
