/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */


import java.util.Arrays;
import java.util.List;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.utils.Debug;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;
import com.exametrika.tests.groups.fail.TestFailureSpec;
import com.exametrika.tests.groups.fail.TestFailureSpec.FailureEventType;
import com.exametrika.tests.groups.fail.TestFailureSpec.FailurePeriodType;
import com.exametrika.tests.groups.fail.TestFailureSpec.FailureQuantityType;
import com.exametrika.tests.groups.fail.TestFailureSpec.FailureTarget;
import com.exametrika.tests.groups.load.TestLoadGroupChannelFactory;
import com.exametrika.tests.groups.load.TestLoadSpec;
import com.exametrika.tests.groups.load.TestLoadSpec.SendFrequencyType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendSourceType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;
import com.exametrika.tests.groups.load.TestLoadSpec.SizeType;

public class MulticastTestMain
{
    public static void main(String[] args) throws Throwable
    {
        int index = Integer.parseInt(args[0]);
        int count = Integer.parseInt(args[1]);
        
        TestLoadSpec loadSpec = new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE);
        List<TestFailureSpec> failureSpecs = Arrays.asList(
            new TestFailureSpec(FailureTarget.RANDOM_NODE, FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, 
                FailurePeriodType.RANDOM, 10000),
            new TestFailureSpec(FailureTarget.COORDINATOR, FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, 
                FailurePeriodType.RANDOM, 60000),
            new TestFailureSpec(FailureTarget.COORDINATOR, FailureQuantityType.SINGLE, 0, FailureEventType.START_FLUSH, 
                FailurePeriodType.RANDOM, 300000));
        
        IChannel channel = createChannel(index, count, loadSpec, failureSpecs);
        
        Debug.print("Press 'q' to exit...");
        while (true)
        {
            if (System.in.read() == 'q')
                break;
        }
        
        channel.stop();
    }
    
    private static IChannel createChannel(int index, int count, TestLoadSpec loadSpec, List<TestFailureSpec> failureSpecs)
    {
        TestGroupFactoryParameters factoryParameters = new TestGroupFactoryParameters();
        TestLoadGroupChannelFactory factory = new TestLoadGroupChannelFactory(factoryParameters);
        return factory.create(index, count, loadSpec, failureSpecs);
    }
}
