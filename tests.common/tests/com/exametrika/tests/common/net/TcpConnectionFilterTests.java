/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.net;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannel.Parameters;
import com.exametrika.common.net.ITcpChannelAcceptor;
import com.exametrika.common.net.ITcpChannelReader;
import com.exametrika.common.net.ITcpChannelWriter;
import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.utils.TcpAndConnectionFilter;
import com.exametrika.common.net.utils.TcpCountPerNodeConnectionFilter;
import com.exametrika.common.net.utils.TcpMaxCountConnectionFilter;
import com.exametrika.common.net.utils.TcpNameConnectionFilter;
import com.exametrika.common.net.utils.TcpNameFilter;
import com.exametrika.common.net.utils.TcpOrConnectionFilter;
import com.exametrika.common.utils.Assert;



/**
 * The {@link TcpConnectionFilterTests} are tests for {@link ITcpConnectionFilter} implementations.
 * 
 * @see TcpAndConnectionFilter
 * @see TcpOrConnectionFilter
 * @see TcpCountPerNodeConnectionFilter
 * @see TcpMaxCountConnectionFilter
 * @see TcpNameConnectionFilter
 * @see TcpNameFilter
 * @author Medvedev-A
 */
public class TcpConnectionFilterTests
{
    @Test
    public void testAndConnectionFilter()
    {
        TestConnectionFilter filter1 = new TestConnectionFilter();
        filter1.allow = false;
        TestConnectionFilter filter2 = new TestConnectionFilter();
        filter2.allow = true;
        TcpAndConnectionFilter filter = new TcpAndConnectionFilter(Arrays.<ITcpConnectionFilter>asList(filter1, filter2));
        assertThat(filter.allow(null, null), is(false));
        
        filter1.allow = true;
        assertThat(filter.allow(null, null), is(true));
    }
    
    @Test
    public void testOrConnectionFilter()
    {
        TestConnectionFilter filter1 = new TestConnectionFilter();
        filter1.allow = false;
        TestConnectionFilter filter2 = new TestConnectionFilter();
        filter2.allow = true;
        TcpOrConnectionFilter filter = new TcpOrConnectionFilter(Arrays.<ITcpConnectionFilter>asList(filter1, filter2));
        assertThat(filter.allow(null, null), is(true));
        
        filter2.allow = false;
        assertThat(filter.allow(null, null), is(false));
    }
    
    @Test
    public void testMaxCountConnectionFilter()
    {
        TcpMaxCountConnectionFilter filter = new TcpMaxCountConnectionFilter(2);
        assertThat(filter.allow(null, Arrays.<InetSocketAddress>asList((InetSocketAddress)null)), is(true));
        assertThat(filter.allow(null, Arrays.<InetSocketAddress>asList((InetSocketAddress)null, (InetSocketAddress)null)), is(false));
    }
    
    @Test
    public void testCountPerNodeConnectionFilter() throws Throwable
    {
        TcpCountPerNodeConnectionFilter filter = new TcpCountPerNodeConnectionFilter(1);
        InetSocketAddress address1 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), 5000);
        InetSocketAddress address2 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{2, 1, 1, 1}), 5000);
        assertThat(filter.allow(address1, Arrays.asList(address2)), is(true));
        assertThat(filter.allow(address1, Arrays.asList(address1)), is(false));
    }
    
    @Test
    public void testNameFilter() throws Throwable
    {
        TcpNameFilter filter1 = new TcpNameFilter("##*.[mco][mco][mco]:8\\d", false);
        assertThat(filter1.match(addr("google.com:80")), is(true));
        assertThat(filter1.match(addr("google.com:81")), is(true));
        assertThat(filter1.match(addr("google.com:21")), is(false));
        assertThat(filter1.match(addr("gazeta.ru:80")), is(false));
        
        TcpNameFilter filter2 = new TcpNameFilter("ip:*.12:8?", false);
        assertThat(filter2.match(addr("1.1.1.12:80")), is(true));
        assertThat(filter2.match(addr("1.1.1.12:20")), is(false));
        assertThat(filter2.match(addr("1.1.1.10:80")), is(false));
        assertThat(filter2.match(addr("2.2.2.12:82")), is(true));
        
        TcpNameFilter filter3 = new TcpNameFilter("ip:1.1.1.12:80", false);
        assertThat(filter3.match(addr("1.1.1.12:80")), is(true));
        assertThat(filter3.match(addr("1.1.1.12:81")), is(false));
        assertThat(filter3.match(addr("1.1.1.10:80")), is(false));
        
        TcpNameFilter filter4 = new TcpNameFilter("google.com:80", false);
        assertThat(filter4.match(addr("google.com:80")), is(true));
        assertThat(filter4.match(addr("google.com:81")), is(false));
        assertThat(filter4.match(addr("gazeta.ru:80")), is(false));
        
        TcpNameFilter filter5 = new TcpNameFilter("#[gole.cm]*:8\\d", false);
        assertThat(filter5.match(addr("google.com:80")), is(true));
        assertThat(filter5.match(addr("google.com:21")), is(false));
        assertThat(filter5.match(addr("gazeta.ru:80")), is(false));
        
        TcpNameFilter filter6 = new TcpNameFilter("google.com:80", false);
        TcpNameFilter filter7 = new TcpNameFilter("google.com:21", false);
        TcpNameFilter filter8 = new TcpNameFilter("gazeta.ru:80", false);
        TcpNameFilter filter9 = new TcpNameFilter("gazeta.ru:21", false);
        
        TcpNameFilter filter10 = new TcpNameFilter("*", false, Arrays.<TcpNameFilter>asList(filter6, filter8), 
            Arrays.<TcpNameFilter>asList(filter7, filter9));
        assertThat(filter10.match(addr("google.com:80")), is(true));
        assertThat(filter10.match(addr("google.com:21")), is(false));
        assertThat(filter10.match(addr("gazeta.ru:80")), is(true));
        assertThat(filter10.match(addr("gazeta.ru:21")), is(false));
    }
    
    @Test
    public void testAdminFilter() throws Throwable
    {
        TcpNioDispatcher dispatcher1 = new TcpNioDispatcher(0, 0, "node" + 0);
        TcpNioDispatcher dispatcher2 = new TcpNioDispatcher(0, 0, "node" + 0);
        
        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.name = "node" + 0;
        compartmentParameters.dispatchPeriod = 100;
        compartmentParameters.dispatcher = dispatcher1;
        ICompartment compartment1 = new CompartmentFactory().createCompartment(compartmentParameters);
        compartmentParameters.dispatcher = dispatcher2;
        ICompartment compartment2 = new CompartmentFactory().createCompartment(compartmentParameters);
        
        final ITcpPacketChannel.Parameters parameters = new ITcpPacketChannel.Parameters();
        parameters.channelReader = new ITcpChannelReader()
        {
            @Override
            public void onRead(ITcpChannel channel)
            {
                ((ITcpPacketChannel)channel).read();
            }
            
            @Override
            public boolean canRead(ITcpChannel channel)
            {
                return true;
            }
        };
        
        parameters.channelWriter = new ITcpChannelWriter()
        {
            
            @Override
            public void onWrite(ITcpChannel channel)
            {
            }
            
            @Override
            public boolean canWrite(ITcpChannel channel)
            {
                return false;
            }
        };

        ITcpServer.Parameters serverParameters = new ITcpServer.Parameters();
        serverParameters.bindAddress = InetAddress.getLocalHost();
        serverParameters.adminFilter = new TcpNameFilter(serverParameters.bindAddress.getCanonicalHostName() + "*:*", true);
        TestConnectionFilter filter = new TestConnectionFilter();
        serverParameters.connectionFilter = filter;
        serverParameters.channelAcceptor = new ITcpChannelAcceptor()
        {
            @Override
            public Parameters accept(InetSocketAddress remoteAddress)
            {
                return parameters;
            }
        };
        ITcpServer server1 = dispatcher1.createServer(serverParameters);

        compartment1.start();
        compartment2.start();
        
        ITcpChannel client1 = dispatcher2.createClient(server1.getLocalAddress(), null, parameters);

        Thread.sleep(1000);
        
        assertThat(client1.isConnected(), is(true));
    }
    
    @Test
    public void testConnectionFilter() throws Throwable
    {
        TcpNioDispatcher dispatcher1 = new TcpNioDispatcher(0, 0, "node" + 0);
        TcpNioDispatcher dispatcher2 = new TcpNioDispatcher(0, 0, "node" + 0);
        
        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.name = "node" + 0;
        compartmentParameters.dispatchPeriod = 100;
        compartmentParameters.dispatcher = dispatcher1;
        ICompartment compartment1 = new CompartmentFactory().createCompartment(compartmentParameters);
        compartmentParameters.dispatcher = dispatcher2;
        ICompartment compartment2 = new CompartmentFactory().createCompartment(compartmentParameters);
        
        final ITcpPacketChannel.Parameters parameters = new ITcpPacketChannel.Parameters();
        parameters.channelReader = new ITcpChannelReader()
        {
            @Override
            public void onRead(ITcpChannel channel)
            {
                ((ITcpPacketChannel)channel).read();
            }
            
            @Override
            public boolean canRead(ITcpChannel channel)
            {
                return true;
            }
        };
        
        parameters.channelWriter = new ITcpChannelWriter()
        {
            
            @Override
            public void onWrite(ITcpChannel channel)
            {
            }
            
            @Override
            public boolean canWrite(ITcpChannel channel)
            {
                return false;
            }
        };
        

        ITcpServer.Parameters serverParameters = new ITcpServer.Parameters();
        TestConnectionFilter filter = new TestConnectionFilter();
        serverParameters.connectionFilter = filter;
        serverParameters.channelAcceptor = new ITcpChannelAcceptor()
        {
            @Override
            public Parameters accept(InetSocketAddress remoteAddress)
            {
                return parameters;
            }
        };
        ITcpServer server1 = dispatcher1.createServer(serverParameters);

        compartment1.start();
        compartment2.start();

        ITcpChannel client1 = dispatcher2.createClient(server1.getLocalAddress(), null, parameters);
        
        Thread.sleep(300);
        
        assertThat(client1.isConnected(), is(false));
        
        filter.allow = true;
        
        client1 = dispatcher2.createClient(server1.getLocalAddress(), null, parameters);
        
        Thread.sleep(300);
        
        assertThat(client1.isConnected(), is(true));
    }
    
    private InetSocketAddress addr(String s)
    {
        int pos = s.indexOf(":");
        Assert.isTrue(pos != -1);
        
        String hostName = s.substring(0, pos);
        int port = Integer.parseInt(s.substring(pos + 1));
        
        try
        {
            return new InetSocketAddress(InetAddress.getByName(hostName), port);
        }
        catch (UnknownHostException e)
        {
            throw new ChannelException(e);
        }
    }
    
    private static class TestConnectionFilter implements ITcpConnectionFilter
    {
        private boolean allow;
        
        @Override
        public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
        {
            return allow;
        }
    }
}
