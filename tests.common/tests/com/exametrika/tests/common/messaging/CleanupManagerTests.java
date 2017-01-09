/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.CleanupManager;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.tests.Tests;
import com.exametrika.tests.common.messaging.AbstractProtocolTests.ProtocolMock;
import com.exametrika.tests.common.messaging.AbstractProtocolTests.TestLiveNodeProvider;
import com.exametrika.tests.common.time.TimeServiceMock;

/**
 * The {@link CleanupManagerTests} are tests for {@link CleanupManager}.
 * 
 * @see CleanupManager
 * @author Medvedev-A
 */
public class CleanupManagerTests
{
    @Test
    public void testManager() throws Exception
    {
        TestAddress address1 = new TestAddress(UUID.randomUUID(), "test1");
        TestAddress address2 = new TestAddress(UUID.randomUUID(), "test2");
        TestAddress address3 = new TestAddress(UUID.randomUUID(), "test3");
        
        TimeServiceMock timeService = new TimeServiceMock();
        timeService.useSystemTime = false;
        
        TestLiveNodeProvider liveNodeProvider = new TestLiveNodeProvider();
        liveNodeProvider.liveNodes = Arrays.<IAddress>asList(address1);
        
        CleanupProtocolMock protocol1 = new CleanupProtocolMock("protocol1");
        protocol1.address = address1;
        CleanupProtocolMock protocol2 = new CleanupProtocolMock("protocol2");
        protocol2.address = address2;
        CleanupProtocolMock protocol3 = new CleanupProtocolMock("protocol3");
        protocol3.address = address3;
        
        CleanupManager manager = new CleanupManager(Arrays.<AbstractProtocol>asList(protocol1, protocol2, protocol3), 
            liveNodeProvider, 1000, 10000);
        manager.setTimeService(timeService);
        
        timeService.time = 100;
        manager.onTimer(100);
        assertThat(protocol1.cleanup, is(true));
        assertThat(protocol2.cleanup, is(true));
        assertThat(protocol3.cleanup, is(true));
        protocol1.cleanup = false;
        protocol2.cleanup = false;
        protocol3.cleanup = false;
        
        timeService.time = 500;
        manager.onTimer(500);
        assertThat(protocol1.cleanup, is(false));
        assertThat(protocol2.cleanup, is(false));
        assertThat(protocol3.cleanup, is(false));
        
        timeService.time = 2000;
        manager.onTimer(2000);
        assertThat(protocol1.cleanup, is(true));
        assertThat(protocol1.canCleanup, is(false));
        assertThat(protocol2.cleanup, is(true));
        assertThat(protocol2.canCleanup, is(false));
        assertThat(protocol3.cleanup, is(true));
        assertThat(protocol3.canCleanup, is(false));
        
        liveNodeProvider.liveNodes = Arrays.<IAddress>asList(address1, address2);
        
        timeService.time = 11000;
        manager.onTimer(11000);
        assertThat(protocol1.canCleanup, is(false));
        assertThat(protocol2.canCleanup, is(false));
        assertThat(protocol3.canCleanup, is(true));
        
        assertThat(((Map)Tests.get(manager, "cleanupInfos")).isEmpty(), is(true));
    }
    
    private static class CleanupProtocolMock extends ProtocolMock
    {
        private IAddress address;
        private boolean canCleanup;
        
        public CleanupProtocolMock(String name)
        {
            super(name);
        }
        
        @Override
        public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
        {
            super.cleanup(cleanupManager, liveNodeProvider, currentTime);
            canCleanup = cleanupManager.canCleanup(address);
        }
    }
}
