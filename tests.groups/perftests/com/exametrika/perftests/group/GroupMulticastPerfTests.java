/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Threads;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;
import com.exametrika.tests.groups.load.TestFailureSpec;
import com.exametrika.tests.groups.load.TestLoadGroupChannelFactory;
import com.exametrika.tests.groups.load.TestLoadSpec;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureEventType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailurePeriodType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureQuantityType;
import com.exametrika.tests.groups.load.TestFailureSpec.FailureTarget;
import com.exametrika.tests.groups.load.TestLoadSpec.SendFrequencyType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendSourceType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;
import com.exametrika.tests.groups.load.TestLoadSpec.SizeType;

public class GroupMulticastPerfTests
{
    private final static int COUNT = 10;
    
    @Test
    public void test()
    {
        testGroup(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testGroup(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
        
        testGroup(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testGroup(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
        
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.SINGLE_NODE));
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES));
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.PULLABLE, SendSourceType.ALL_NODES));
    }
    
    @Test
    public void testFailure()
    {
        testGroup(new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        testGroup(new TestLoadSpec(SizeType.MEDIUM, 0, SizeType.MEDIUM, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
        
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, 
                FailureQuantityType.SINGLE, 0, FailureEventType.START_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, 
                FailureQuantityType.SINGLE, 0, FailureEventType.PROCESSING_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SET, 3, FailureEventType.START_FLUSH, FailurePeriodType.RANDOM, 1000)));
        
        testGroup(new TestLoadSpec(SizeType.LARGE, 0, SizeType.LARGE, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.ALL_NODES), Arrays.asList(new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, 
                FailureQuantityType.SET, 3, FailureEventType.RANDOM, FailurePeriodType.RANDOM, 1000)));
    }
    
    private void testGroup(TestLoadSpec loadSpec)
    {
        testGroup(loadSpec, Collections.<TestFailureSpec>emptyList());
    }
    
    private void testGroup(TestLoadSpec loadSpec, List<TestFailureSpec> failureSpecs)
    {
        Debug.print("------ Test started: " + loadSpec);
        
        List<IChannel> channels = new ArrayList<IChannel>();
        for (int i = 0; i < COUNT; i++)
            channels.add(createChannel(i, COUNT, loadSpec, failureSpecs));
        
        Threads.sleep(300000);
        
        for (IChannel channel : channels)
            channel.stop();
        
        Debug.print("------ Test completed: " + loadSpec);
    }
    
    private IChannel createChannel(int index, int count, TestLoadSpec loadSpec, List<TestFailureSpec> failureSpecs)
    {
        TestGroupFactoryParameters factoryParameters = new TestGroupFactoryParameters();
        TestLoadGroupChannelFactory factory = new TestLoadGroupChannelFactory(factoryParameters);
        return factory.create(index, count, loadSpec, failureSpecs);
    }
}
