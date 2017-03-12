/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Collections;


/**
 * The {@link LiveNodeManagerTests} are tests for {@link LiveNodeManager}.
 * 
 * @see LiveNodeManager
 * @author Medvedev-A
 */
public class LiveNodeManagerTests
{
    private ChannelObserver channelObserver;
    private LiveNodeManager manager;
    private IAddress localNode;
    
    @Before
    public void setUp()
    {
        channelObserver = new ChannelObserver("test");
        manager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        localNode = manager.getLocalNode();
        
        manager.start();
        channelObserver.start();
    }
    
    @After
    public void tearDown()
    {
        channelObserver.stop();
        manager.stop();
    }
    
    @Test
    public void testManager() throws Throwable
    {
        TestChannelListener listener = new TestChannelListener(null);
        channelObserver.addChannelListener(listener);
        
        assertThat(manager.getLocalNode() == localNode, is(true));
        assertThat(manager.getId(), is(0l));
        assertThat(manager.getLiveNodes(), is((List)Arrays.asList(localNode)));
        assertThat(manager.isLive(localNode), is(true));
        assertThat(manager.findById(localNode.getId()) == localNode, is(true));
        assertThat(manager.findByName(localNode.getName()) == localNode, is(true));
        
        IAddress test1 = new TestAddress(UUID.randomUUID(), "test1");
        IAddress test2 = new TestAddress(UUID.randomUUID(), "test2");
        manager.onNodesConnected(Collections.asSet(test1));
        manager.onNodesConnected(Collections.asSet(test1));
        manager.onNodesConnected(Collections.asSet(test2));
        manager.onNodesConnected(Collections.asSet(test2));
        
        Thread.sleep(100);
        
        assertThat(manager.getId(), is(2l));
        assertThat(manager.getLiveNodes(), is((List)Arrays.asList(localNode, test1, test2)));
        assertThat(manager.isLive(test1), is(true));
        assertThat(manager.isLive(test2), is(true));
        assertThat(manager.findById(test1.getId()) == test1, is(true));
        assertThat(manager.findByName(test1.getName()) == test1, is(true));
        assertThat(manager.findById(test2.getId()) == test2, is(true));
        assertThat(manager.findByName(test2.getName()) == test2, is(true));
        
        assertThat(listener.connected, is(Arrays.asList(test1, test2)));
        
        manager.onNodesFailed(Collections.asSet(test1));
        manager.onNodesFailed(Collections.asSet(test1));
        manager.onNodesLeft(Collections.asSet(test2));
        manager.onNodesLeft(Collections.asSet(test2));
        
        Thread.sleep(100);
        
        assertThat(manager.getId(), is(5l));
        assertThat(manager.getLiveNodes(), is((List)Arrays.asList(localNode)));
        assertThat(manager.isLive(test1), is(false));
        assertThat(manager.isLive(test2), is(false));
        assertThat(manager.findById(test1.getId()), nullValue());
        assertThat(manager.findByName(test1.getName()), nullValue());
        assertThat(manager.findById(test2.getId()), nullValue());
        assertThat(manager.findByName(test2.getName()), nullValue());
        
        assertThat(listener.failed, is(Arrays.asList(test1, test1)));
        assertThat(listener.disconnected, is(Arrays.asList(test2)));
        
        listener.connected.clear();
        listener.failed.clear();
        listener.disconnected.clear();
        
        channelObserver.removeChannelListener(listener);
        
        manager.onNodesConnected(Collections.asSet(test1));
        
        Thread.sleep(100);
        
        assertThat(manager.getId(), is(6l));
        assertThat(listener.connected.isEmpty(), is(true));
        assertThat(listener.failed.isEmpty(), is(true));
        assertThat(listener.disconnected.isEmpty(), is(true));
        
        manager.onNodesConnected(Collections.asSet(test1));
        manager.onNodesFailed(Collections.asSet(test2));
        manager.onNodesLeft(Collections.asSet(test2));
        
        Thread.sleep(100);
        
        assertThat(manager.getId(), is(7l));
    }
    
    public static class TestChannelListener implements IChannelListener
    {
        public List<IAddress> connected = new ArrayList<IAddress>();
        public List<IAddress> disconnected = new ArrayList<IAddress>();
        public List<IAddress> failed = new ArrayList<IAddress>();
        private final Sequencer sequencer;
        
        public TestChannelListener(Sequencer sequencer)
        {
            this.sequencer = sequencer;
        }
        
        @Override
        public synchronized void onNodeConnected(IAddress node)
        {
            connected.add(node);
            if (sequencer != null)
                sequencer.allowSingle("Connected " + node);
        }
    
        @Override
        public synchronized void onNodeFailed(IAddress node)
        {
            failed.add(node);
        }
    
        @Override
        public synchronized void onNodeDisconnected(IAddress node)
        {
            disconnected.add(node);
        }
    }
}
