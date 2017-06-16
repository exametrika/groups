/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannel;
import com.exametrika.impl.groups.cluster.failuredetection.CoreClusterFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.WorkerClusterFailureDetectionProtocol;
import com.exametrika.tests.groups.load.TestLoadMessagePart;

public class ClusterFailureDetectionTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    
    @Before
    public void setUp()
    {
        createCluster(CORE_NODE_COUNT, WORKER_NODE_COUNT);
    }
    
    @After
    public void tearDown()
    {
        stopCluster();
    }
    
    @Test
    public void testWorkerFailure()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        workerChannels.get(0).stop();
        workerChannels.get(1).stop();
        
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(Collections.asSet(0, 1));
    }
    
    @Test
    public void testControllerFailure()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel worker1 = workerChannels.get(0);
        worker1.stop();
        WorkerNodeChannel worker2 = workerChannels.get(1);
        worker2.stop();
        CoreNodeChannel controller = coreChannels.get(findController(worker1.getMembershipService().getLocalNode()));       
        controller.stop();
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(Collections.asSet(0, 1));
    }
    
    @Test
    public void testCoordinatorFailure()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel worker1 = workerChannels.get(0);
        worker1.stop();
        WorkerNodeChannel worker2 = workerChannels.get(1);
        worker2.stop();
        CoreNodeChannel coordinator = coreChannels.get(findCoordinator());       
        coordinator.stop();
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(Collections.asSet(0, 1));
    }
    
    @Test
    public void testOrphanedWorkerReconnect()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel worker1 = workerChannels.get(0);
        CoreNodeChannel controller = coreChannels.get(findController(worker1.getMembershipService().getLocalNode()));       
        controller.stop();
        Threads.sleep(5000);
        
        assertTrue(reconnections.contains(0));
    }
    
    @Test
    public void testFailureHistory() throws Throwable
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel worker1 = workerChannels.get(0);
        worker1.stop();
        Threads.sleep(2000);
        
        WorkerClusterFailureDetectionProtocol protocol = ((SubChannel)workerChannels.get(0).getMainSubChannel()
            ).getProtocolStack().find(WorkerClusterFailureDetectionProtocol.class);
        Map failureHistory = Tests.get(protocol, "failureHistory");
        assertThat(failureHistory.size(), is(1));
        assertTrue(failureHistory.keySet().contains(worker1.getMembershipService().getLocalNode().getAddress()));
        
        Threads.sleep(4000);
        assertTrue(failureHistory.isEmpty());
    }
    
    @Test
    public void testShun() throws Throwable
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel worker1 = workerChannels.get(0);
        CoreNodeChannel controller = coreChannels.get(findController(worker1.getMembershipService().getLocalNode()));       
        CoreClusterFailureDetectionProtocol protocol = ((SubChannel)controller.getMainSubChannel()
                ).getProtocolStack().find(CoreClusterFailureDetectionProtocol.class);
        protocol.onNodesFailed(Collections.asSet(worker1.getMembershipService().getLocalNode().getAddress()));
        Threads.sleep(2000);
        
        worker1.send(worker1.getMainSubChannel().getMessageFactory().create(workerChannels.get(1).getMembershipService().getLocalNode().getAddress(), 
             new TestLoadMessagePart(0, 0, new ByteArray(new byte[0]))));
        Threads.sleep(200);
        assertTrue(reconnections.contains(0));
    }
}
