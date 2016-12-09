/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.protocols.failuredetection.CoordinatorCohortNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.FullNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.RandomNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.RingNodeTrackingStrategy;
import com.exametrika.common.tests.Expected;

/**
 * The {@link NodeTrackingStrategiesTests} are tests for {@link INodeTrackingStrategy} implementations.
 * 
 * @see CoordinatorCohortNodeTrackingStrategy
 * @see RingNodeTrackingStrategy
 * @see RandomNodeTrackingStrategy
 * @see FullNodeTrackingStrategy
 * @author Medvedev-A
 */
public class NodeTrackingStrategiesTests
{
    @Test
    public void testCoordinatorCohortStrategy() throws Throwable
    {
        final IAddress member1 = new TestAddress(UUID.randomUUID(), "member1");
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");
        IAddress member3 = new TestAddress(UUID.randomUUID(), "member3");
        IAddress member4 = new TestAddress(UUID.randomUUID(), "member4");
        IAddress member5 = new TestAddress(UUID.randomUUID(), "member5");
        final IAddress member6 = new TestAddress(UUID.randomUUID(), "member6");
        final List<IAddress> members = new ArrayList<IAddress>();
        members.add(member1);
        members.add(member2);
        members.add(member3);
        members.add(member4);
        members.add(member5);
        
        final CoordinatorCohortNodeTrackingStrategy strategy = new CoordinatorCohortNodeTrackingStrategy();
        Set<IAddress> coordinatorTrackedNodes = strategy.getTrackedNodes(member1, members);
        assertThat(coordinatorTrackedNodes.size(), is(members.size() - 1));
        for (int i = 1; i < members.size(); i++)
            assertThat(coordinatorTrackedNodes.contains(members.get(i)), is(true));
        
        Set<IAddress> cohortTrackedNodes = strategy.getTrackedNodes(member2, members);
        assertThat(cohortTrackedNodes.size(), is(1));
        assertThat(cohortTrackedNodes.contains(member1), is(true));
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member1, Collections.<IAddress>emptyList());
                
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member6, members);
                
            }
        });
    }
    
    @Test
    public void testSmallRingStrategy() throws Throwable
    {
        final IAddress member1 = new TestAddress(UUID.randomUUID(), "member1");
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");
        IAddress member3 = new TestAddress(UUID.randomUUID(), "member3");
        IAddress member4 = new TestAddress(UUID.randomUUID(), "member4");
        IAddress member5 = new TestAddress(UUID.randomUUID(), "member5");
        final IAddress member6 = new TestAddress(UUID.randomUUID(), "member6");
        final List<IAddress> members = new ArrayList<IAddress>();
        members.add(member1);
        members.add(member2);
        members.add(member3);
        members.add(member4);
        members.add(member5);
        
        final RingNodeTrackingStrategy strategy = new RingNodeTrackingStrategy();
        Set<IAddress> trackedNodes = strategy.getTrackedNodes(member1, members);
        assertThat(trackedNodes.size(), is(1));
        assertThat(trackedNodes.contains(member2), is(true));
        
        trackedNodes = strategy.getTrackedNodes(member5, members);
        assertThat(trackedNodes.size(), is(1));
        assertThat(trackedNodes.contains(member1), is(true));
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member1, Collections.<IAddress>emptyList());
                
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member6, members);
                
            }
        });
    }
    
    @Test
    public void testLargeRingStrategy() throws Throwable
    {
        List<IAddress> members = new ArrayList<IAddress>();
        for (int i = 0; i < RingNodeTrackingStrategy.SINGLE_RING_THRESHOLD + 7; i++)
            members.add(new TestAddress(UUID.randomUUID(), "member" + i));
        
        int[][] rings = {{1, 5}, {2}, {3}, {4}, {0}, {6, 9}, {7}, {8}, {5}, {10, 13}, {11}, {12}, {9}, {14, 0}, {15}, {16}, {13}};
            
        RingNodeTrackingStrategy strategy = new RingNodeTrackingStrategy();
        for (int i = 0; i < members.size(); i++)
        {
            Set<IAddress> trackedNodes = strategy.getTrackedNodes(members.get(i), members);
            Set<IAddress> expectedNodes = new HashSet<IAddress>();
            for (int k = 0; k < rings[i].length; k++)
                expectedNodes.add(members.get(rings[i][k]));
            assertThat(trackedNodes, is(expectedNodes));
        }
    }
    
    @Test
    public void testRandomStrategy() throws Throwable
    {
        final IAddress member1 = new TestAddress(UUID.randomUUID(), "member1");
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");
        IAddress member3 = new TestAddress(UUID.randomUUID(), "member3");
        IAddress member4 = new TestAddress(UUID.randomUUID(), "member4");
        IAddress member5 = new TestAddress(UUID.randomUUID(), "member5");
        final IAddress member6 = new TestAddress(UUID.randomUUID(), "member6");
        final List<IAddress> members = new ArrayList<IAddress>();
        members.add(member1);
        members.add(member2);
        members.add(member3);
        members.add(member4);
        members.add(member5);
        
        final RandomNodeTrackingStrategy strategy = new RandomNodeTrackingStrategy(2);
        Set<IAddress> trackedNodes = strategy.getTrackedNodes(member1, members);
        assertThat(trackedNodes.size(), is(2));
        for (IAddress node : trackedNodes)
        {
            assertThat(members.contains(node), is(true));
            assertThat(member1.equals(node), is(false));
        }
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member1, Collections.<IAddress>emptyList());
                
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                strategy.getTrackedNodes(member6, members);
                
            }
        });
        
        RandomNodeTrackingStrategy strategy2 = new RandomNodeTrackingStrategy(10);
        trackedNodes = strategy2.getTrackedNodes(member1, members);
        assertThat(trackedNodes.size(), is(members.size() - 1));
        members.remove(0);
        assertThat(trackedNodes.containsAll(members), is(true));
    }
    
    @Test
    public void testFullStrategy() throws Throwable
    {
        final IAddress member1 = new TestAddress(UUID.randomUUID(), "member1");
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");
        IAddress member3 = new TestAddress(UUID.randomUUID(), "member3");
        final List<IAddress> members = new ArrayList<IAddress>();
        members.add(member1);
        members.add(member2);
        members.add(member3);
        
        final FullNodeTrackingStrategy strategy = new FullNodeTrackingStrategy();
        Set<IAddress> trackedNodes = strategy.getTrackedNodes(member1, members);
        assertThat(trackedNodes.size(), is(2));
        for (IAddress node : trackedNodes)
        {
            assertThat(members.contains(node), is(true));
            assertThat(member1.equals(node), is(false));
        }
    }
}
