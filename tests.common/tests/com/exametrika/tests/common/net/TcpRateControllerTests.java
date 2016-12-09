/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.net;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;

import org.junit.Test;

import com.exametrika.common.log.IMarker;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpDispatcher;
import com.exametrika.common.net.ITcpRateController;
import com.exametrika.common.net.utils.TcpRateController;



/**
 * The {@link TcpRateControllerTests} are tests for {@link ITcpRateController} implementations.
 * 
 * @see TcpRateController
 * @author Medvedev-A
 */
public class TcpRateControllerTests
{
    @Test
    public void testRateController()
    {
        DispatcherMock dispatcher = new DispatcherMock();
        ChannelMock channel = new ChannelMock();
        channel.dispatcher = dispatcher;
        
        TcpRateController factory = new TcpRateController(true, 100, 200, true, 1000, 2000, false, 1000, 1);
        ITcpRateController controller = factory.createController(channel);
        
        controller.onTimer(10000);
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(150);
        controller.incrementWriteCount(1500);
        
        controller.onTimer(11000);
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(250);
        controller.incrementWriteCount(2500);
        
        controller.onTimer(12000);
        assertThat(controller.canRead(), is(false));
        assertThat(controller.canWrite(), is(false));
        assertThat(channel.readStatus, is(true));
        assertThat(channel.writeStatus, is(true));
        channel.readStatus = false;
        channel.writeStatus = false;
        
        controller.incrementReadCount(150);
        controller.incrementWriteCount(1500);
        
        controller.onTimer(13000);
        
        assertThat(controller.canRead(), is(false));
        assertThat(controller.canWrite(), is(false));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(50);
        controller.incrementWriteCount(500);
        
        controller.onTimer(14000);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(true));
        assertThat(channel.writeStatus, is(true));
        channel.readStatus = false;
        channel.writeStatus = false;
        
        controller.incrementReadCount(5000);
        controller.incrementWriteCount(500000);
        
        controller.onTimer(14500);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        channel.admin = true;
        
        controller.onTimer(15000);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
    }
    
    @Test
    public void testTotalRateController()
    {
        DispatcherMock dispatcher = new DispatcherMock();
        dispatcher.channelCount = 10;
        ChannelMock channel = new ChannelMock();
        channel.dispatcher = dispatcher;
        
        TcpRateController factory = new TcpRateController(true, 1000, 2000, true, 10000, 20000, true, 1000, 1);
        ITcpRateController controller = factory.createController(channel);
        
        controller.onTimer(10000);
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(150);
        controller.incrementWriteCount(1500);
        
        controller.onTimer(11000);
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(250);
        controller.incrementWriteCount(2500);
        
        controller.onTimer(12000);
        assertThat(controller.canRead(), is(false));
        assertThat(controller.canWrite(), is(false));
        assertThat(channel.readStatus, is(true));
        assertThat(channel.writeStatus, is(true));
        channel.readStatus = false;
        channel.writeStatus = false;
        
        controller.incrementReadCount(150);
        controller.incrementWriteCount(1500);
        
        controller.onTimer(13000);
        
        assertThat(controller.canRead(), is(false));
        assertThat(controller.canWrite(), is(false));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
        
        controller.incrementReadCount(50);
        controller.incrementWriteCount(500);
        
        controller.onTimer(14000);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(true));
        assertThat(channel.writeStatus, is(true));
        channel.readStatus = false;
        channel.writeStatus = false;
        
        controller.incrementReadCount(5000);
        controller.incrementWriteCount(500000);
        
        controller.onTimer(14500);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
    }
    
    @Test
    public void testAdminRateController()
    {
        DispatcherMock dispatcher = new DispatcherMock();
        dispatcher.channelCount = 10;
        ChannelMock channel = new ChannelMock();
        channel.dispatcher = dispatcher;
        channel.admin = true;
        
        TcpRateController factory = new TcpRateController(true, 100, 200, true, 1000, 2000, false, 1000, 1);
        ITcpRateController controller = factory.createController(channel);
        controller.onTimer(10000);
        
        controller.incrementReadCount(100000);
        controller.incrementWriteCount(100000);
        
        controller.onTimer(11000);
        
        assertThat(controller.canRead(), is(true));
        assertThat(controller.canWrite(), is(true));
        assertThat(channel.readStatus, is(false));
        assertThat(channel.writeStatus, is(false));
    }
    
    private static class DispatcherMock implements ITcpDispatcher
    {
        private int serverCount;
        private int channelCount;
        
        @Override
        public int getServerCount()
        {
            return serverCount;
        }

        @Override
        public int getChannelCount()
        {
            return channelCount;
        }
    }
    
    private static class ChannelMock implements ITcpChannel
    {
        private DispatcherMock dispatcher = new DispatcherMock();
        private boolean admin;
        private boolean readStatus;
        private boolean writeStatus;
        
        @Override
        public ITcpDispatcher getDispatcher()
        {
            return dispatcher;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public void setName(String name)
        {
        }

        @Override
        public IMarker getMarker()
        {
            return null;
        }

        @Override
        public <T> T getData()
        {
            return null;
        }

        @Override
        public <T> void setData(T data)
        {
        }

        @Override
        public boolean isConnected()
        {
            return false;
        }

        @Override
        public boolean isDisconnected()
        {
            return false;
        }

        @Override
        public InetSocketAddress getRemoteAddress()
        {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress()
        {
            return null;
        }

        @Override
        public long getLastReadTime()
        {
            return 0;
        }
        
        @Override
        public long getLastWriteTime()
        {
            return 0;
        }

        @Override
        public boolean isAdmin()
        {
            return admin;
        }

        @Override
        public void setAdmin()
        {
        }

        @Override
        public void updateReadStatus()
        {
            readStatus = true;
        }

        @Override
        public void updateWriteStatus()
        {
            writeStatus = true;
        }

        @Override
        public void disconnect()
        {
        }

        @Override
        public void close()
        {
        }
    }
}
