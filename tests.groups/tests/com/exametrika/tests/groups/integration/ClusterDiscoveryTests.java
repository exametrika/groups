/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClusterDiscoveryTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    
    @Before
    public void setUp()
    {
        createCluster(CORE_NODE_COUNT, WORKER_NODE_COUNT);
        startCluster();
    }
    
    @After
    public void tearDown()
    {
        stopCluster();
    }
    
    @Test
    public void testDiscovery()
    {
    }
}
