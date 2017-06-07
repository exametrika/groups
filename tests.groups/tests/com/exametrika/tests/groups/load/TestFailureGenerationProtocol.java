/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;

/**
 * The {@link TestFailureGenerationProtocol} is a protocol which generates failures for group nodes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class TestFailureGenerationProtocol extends AbstractProtocol implements IFlushParticipant
{
    private final List<TestFailureSpec> failureSpecs;
    private final long processPeriod;
    private final IGroupFailureDetector failureDetector;
    private final Random random = new Random();
    private boolean coordinator;
    private IFlush flush;
    private boolean processFlush;
    private IGroupMembership membership;
    private long lastProcessTime;
    private long[] nextFailureTimes;
   
    public TestFailureGenerationProtocol(String channelName, IMessageFactory messageFactory, List<TestFailureSpec> failureSpecs,
        long processPeriod, IGroupFailureDetector failureDetector)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(failureSpecs);
        Assert.notNull(failureDetector);
        
        this.failureSpecs = failureSpecs;
        this.processPeriod = processPeriod;
        this.failureDetector = failureDetector;
        this.nextFailureTimes = new long[failureSpecs.size()];
    }
    
    @Override
    public boolean isFlushProcessingRequired()
    {
        return true;
    }
    
    @Override
    public void setCoordinator()
    {
        coordinator = true;
    }

    @Override
    public void startFlush(IFlush flush)
    {
        this.flush = flush;
        this.membership = flush.getNewMembership();
        flush.grantFlush(this);
        
        if (coordinator)
            processFails(timeService.getCurrentTime());
    }

    @Override
    public void beforeProcessFlush()
    {
    }

    @Override
    public void processFlush()
    {
        processFlush = true;
        flush.grantFlush(this);
        
        if (coordinator)
            processFails(timeService.getCurrentTime());
    }

    @Override
    public void endFlush()
    {
        flush = null;
        processFlush = false;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!coordinator || membership == null)
            return;
        
        if (currentTime > lastProcessTime + processPeriod)
            processFails(currentTime);
    }

    private void processFails(long currentTime)
    {
        Set<INode> failedNodes = getFailedNodes(currentTime);
        for (INode node : failedNodes)
            send(messageFactory.create(node.getAddress(), MessageFlags.SHUN));
        
        lastProcessTime = currentTime;
    }

    private Set<INode> getFailedNodes(long currentTime)
    {
        List<INode> healthyNodes = new ArrayList<INode>(failureDetector.getHealthyMembers());
        
        Set<INode> failedNodes = new HashSet<INode>();
        for (int i = 0; i < failureSpecs.size(); i++)
        {
            TestFailureSpec failureSpec = failureSpecs.get(i);
            if (healthyNodes.isEmpty())
                break;
            
            switch (failureSpec.getFailureEventType())
            {
            case START_FLUSH:
                if (flush == null || processFlush)
                    continue;
                break;
            case PROCESSING_FLUSH:
                if (flush == null || !processFlush)
                    continue;
                break;
            case RANDOM:
                break;
            default:
                Assert.error();
            }
            
            if (currentTime < nextFailureTimes[i])
                continue;
            
            boolean fail = false;
            int count = getFailedNodesCount(failureSpec, healthyNodes.size());
            while (count > 0)
            {
                if (healthyNodes.isEmpty())
                    break;
                
                INode node = null;
                switch(failureSpec.getFailureTarget())
                {
                case RANDOM_NODE:
                    node = healthyNodes.remove(random.nextInt(healthyNodes.size()));
                    break;
                case COORDINATOR:
                    node = failureDetector.getCurrentCoordinator();
                    break;
                }
                
                if (node != null)
                {
                    failedNodes.add(node);
                    fail = true;
                }
                
                count--;
            }
            
            if (fail)
                nextFailureTimes[i] = currentTime + getFailurePeriod(failureSpec);
        }
        
        return failedNodes;
    }
    
    private int getFailedNodesCount(TestFailureSpec failureSpec, int healthyNodesCount)
    {
        switch(failureSpec.getFailureQuantityType())
        {
        case SINGLE:
            return 1;
        case SET:
            return (int)failureSpec.getFailureQuantity();
        case SET_PERCENTAGE:
            return (int)(healthyNodesCount * failureSpec.getFailureQuantity() / 100);
        default:
            return Assert.error();
        }
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
