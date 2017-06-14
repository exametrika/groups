/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;

public class ClusterDiscoveryTests extends AbstractClusterTests
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
    public void testWorkerDiscovery()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkWorkerNodesMembership(null);
    }
    
    @Test
    public void testWorkerDiscoveryWithCoreFailures()
    {
        coreChannels.get(0).start();
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(300);
        
        coreChannels.get(0).stop();
        
        Threads.sleep(1000);
        coreChannels.get(1).start();
        
        Threads.sleep(2000);
        checkWorkerNodesMembership(null);
        
        coreChannels.get(1).stop();
        
        Threads.sleep(2000);
        checkWorkerReconnections(null);
        
        Threads.sleep(1000);
        coreChannels.get(2).start();
        
        Threads.sleep(2000);
        checkWorkerNodesMembership(null);
    }
    
    @Test
    public void testWorkerDiscoveryUsingNonCoordinator()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        wellKnownAddressesIndexes = findNonCoordinators(2);
        startWorkerNodes(null);
        Threads.sleep(200);
        
        CoreNodeChannel channel = coreChannels.get(wellKnownAddressesIndexes.iterator().next());
        channel.stop();
        
        Threads.sleep(2000);
        
        checkWorkerNodesMembership(null);
    }
    
    @Test
    public void testWorkerDiscoveryWithCoordinatorFailure()
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        CoreNodeChannel coordinator = coreChannels.get(findCoordinator());
        startWorkerNodes(null);
        Threads.sleep(200);
        
        coordinator.stop();
        
        Threads.sleep(2000);
        
        checkWorkerNodesMembership(null);
    }
}
