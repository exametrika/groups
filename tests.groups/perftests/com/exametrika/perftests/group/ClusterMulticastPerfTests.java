/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.membership.AddGroupsCommand;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupDefinition;
import com.exametrika.tests.groups.integration.AbstractClusterTests;
import com.exametrika.tests.groups.load.TestCoreNodeChannelFactory;
import com.exametrika.tests.groups.load.TestCoreNodeParameters;
import com.exametrika.tests.groups.load.TestFailureSpec;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureEventType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailurePeriodType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureQuantityType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureTarget;
import com.exametrika.tests.groups.load.TestLoadSpec;
import com.exametrika.tests.groups.load.TestLoadSpec.SendFrequencyType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendSourceType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;
import com.exametrika.tests.groups.load.TestLoadSpec.SizeType;
import com.exametrika.tests.groups.load.TestWorkerNodeChannelFactory;
import com.exametrika.tests.groups.load.TestWorkerNodeParameters;

public class ClusterMulticastPerfTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    protected List<TestFailureSpec> failureSpecs;
    protected GroupDefinition group;
    
    @Test
    public void test() throws Throwable
    {
        testCluster(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testCluster(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
        
        testCluster(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testCluster(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
        
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
    }
    
    @Test
    public void testFailure() throws Throwable
    {
        testCluster(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        testCluster(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, 
                FailureQuantityType.SINGLE, 0, FailureEventType.START_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, 
                FailureQuantityType.SINGLE, 0, FailureEventType.PROCESSING_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SET, 3, FailureEventType.START_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testCluster(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SET, 3, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
    }
    
    private void testCluster(TestLoadSpec loadSpec) throws Throwable
    {
        testCluster(loadSpec, Collections.<TestFailureSpec>emptyList());
    }
    
    private void testCluster(TestLoadSpec loadSpec, List<TestFailureSpec> failureSpecs) throws Throwable
    {
        Debug.print("------ Test started: " + loadSpec);
        
        this.loadSpec = loadSpec;
        this.failureSpecs = failureSpecs;
        group = new GroupDefinition("test", UUID.randomUUID(), "group", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 100, null);
        groupAddress = new GroupAddress(group.getId(), group.getName());
        
        createCluster(CORE_NODE_COUNT, WORKER_NODE_COUNT);
        
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        CommandManager commandManager = findCommandManager(coreChannels.get(0));
        AddGroupsCommand addGroupsCommand = new AddGroupsCommand(Arrays.asList(group));
        SyncCompletionHandler completionHandler = new SyncCompletionHandler();
        commandManager.execute(addGroupsCommand, completionHandler);
        completionHandler.await(5000);
        
        checkWorkerGroupsMembership(buildGroupDefinitionsMap(Arrays.asList(group)), null);
        
        Threads.sleep(300000);
        
        stopCluster();
        
        Debug.print("------ Test completed: " + loadSpec);
    }
    
    @Override
    protected CoreNodeParameters createCoreNodeParameters(int index, int count)
    {
        TestCoreNodeParameters parameters = (TestCoreNodeParameters)super.createCoreNodeParameters(index, count);
        parameters.failureSpecs = failureSpecs;
        return parameters;
    }
    
    @Override
    protected CoreNodeParameters createCoreNodeParameters()
    {
        TestCoreNodeParameters parameters = new TestCoreNodeParameters();
        return parameters;
    }
    
    @Override
    protected WorkerNodeParameters createWorkerNodeParameters(int index, int count)
    {
        TestWorkerNodeParameters parameters = (TestWorkerNodeParameters)super.createWorkerNodeParameters(index, count);
        parameters.failureSpecs = failureSpecs;
        return parameters;
    }
    
    @Override
    protected WorkerNodeParameters createWorkerNodeParameters()
    {
        TestWorkerNodeParameters parameters = new TestWorkerNodeParameters();
        return parameters;
    }
    
    @Override
    protected CoreNodeChannelFactory createCoreNodeFactory()
    {
        TestCoreNodeChannelFactory factory = new TestCoreNodeChannelFactory();
        return factory;
    }

    @Override
    protected WorkerNodeChannelFactory createWorkerNodeFactory()
    {
        TestWorkerNodeChannelFactory factory = new TestWorkerNodeChannelFactory();
        return factory;
    }
}
