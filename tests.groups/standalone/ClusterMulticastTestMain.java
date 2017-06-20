/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */


import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INodeChannel;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.membership.AddGroupsCommand;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupDefinition;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.perftests.group.ClusterMulticastPerfTests;
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

public class ClusterMulticastTestMain extends ClusterMulticastPerfTests
{
    public static void main(String[] args) throws Throwable
    {
        int index = Integer.parseInt(args[0]);
        int count = Integer.parseInt(args[1]);
        boolean core = Boolean.parseBoolean(args[2]);
        
        ClusterMulticastTestMain tests = new ClusterMulticastTestMain();
        tests.run(index, count, core);
    }
    
    private void run(int index, int count, boolean core) throws Throwable
    {
        group = new GroupDefinition("test", UUID.fromString("ea2e64f2-90f1-4eb6-aaaf-802411b252bf"), "group", 
            Enums.of(GroupOption.DURABLE, GroupOption.ASYNC_STATE_TRANSFER, GroupOption.CHECK_STATE), null, 100, null);
        groupAddress = new GroupAddress(group.getId(), group.getName());
        
        loadSpec = new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE);
        failureSpecs = Arrays.asList(
            new TestFailureSpec(FailureTarget.RANDOM_GROUP_NODE, FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, 
                FailurePeriodType.RANDOM, 10000),
            new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, FailureQuantityType.SINGLE, 0, FailureEventType.RANDOM, 
                FailurePeriodType.RANDOM, 60000),
            new TestFailureSpec(FailureTarget.GROUP_COORDINATOR, FailureQuantityType.SINGLE, 0, FailureEventType.START_FLUSH, 
                FailurePeriodType.RANDOM, 300000));
        
        INodeChannel channel;
        if (core)
        {
            CoreNodeParameters parameters = createCoreNodeParameters(index, count);
            channel = createCoreChannel(parameters);
        }
        else
        {
            WorkerNodeParameters parameters = createWorkerNodeParameters(index, count);
            channel = createWorkerChannel(parameters);
        }
        
        channel.start();
        
        Threads.sleep(10000);
        IClusterMembership membership = channel.getMembershipService().getMembership();
        GroupsMembership groupsMembership = membership.findDomain("test").findElement(GroupsMembership.class);
        IGroup group = groupsMembership.findGroup(this.group.getId());
        if (core && group.getCoordinator().equals(channel.getMembershipService().getLocalNode()))
        {
            final CommandManager commandManager = findCommandManager((CoreNodeChannel)channel);
            final AddGroupsCommand addGroupsCommand = new AddGroupsCommand(Arrays.asList(this.group));
            final SyncCompletionHandler completionHandler = new SyncCompletionHandler();
            new ScheduledThreadPoolExecutor(1).schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    commandManager.execute(addGroupsCommand, completionHandler);
                    completionHandler.await(5000);
                    
                }
            }, 30, TimeUnit.SECONDS);
        }
        
        Debug.print("Press 'q' to exit...");
        while (true)
        {
            if (System.in.read() == 'q')
                break;
        }
        
        channel.stop();
    }
}
