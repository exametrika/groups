/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.net;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelAcceptor;
import com.exametrika.common.net.ITcpChannelHandshaker;
import com.exametrika.common.net.ITcpChannelListener;
import com.exametrika.common.net.ITcpChannelReader;
import com.exametrika.common.net.ITcpChannelWriter;
import com.exametrika.common.net.ITcpFactory;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpPacketSerializer;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.TcpAbstractChannel;
import com.exametrika.common.net.TcpException;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.nio.TcpNioPacketChannel;
import com.exametrika.common.net.nio.TcpNioServer;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;
import com.exametrika.common.net.nio.ssl.TcpSslSocketChannelFactory;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.IOs;

/**
 * The {@link TcpChannelTests} are tests for {@link TcpNioPacketChannel},
 * {@link TcpNioDispatcher} and {@link TcpNioServer}.
 * 
 * @see TcpNioPacketChannel
 * @see TcpNioDispatcher
 * @see TcpNioServer
 * @author Medvedev-A
 */
@RunWith(Parameterized.class)
public class TcpChannelTests
{
    private static final boolean DEBUG = false;
    private static final int COUNT = 10;
    private final boolean secured;
    private TcpNioDispatcher[] dispatchers;
    private ICompartment[] compartments;
    private ITcpChannel[] clients;
    private ITcpChannel[] acceptedChannels;
    private ITcpServer[] servers;
    private ByteArray packet;
    private TcpPacket multiPacket;
    private final int packetSize;
    private List<File> fileNames = new ArrayList<File>();

    @Parameters
    public static Collection<?> parameters()
    {
        return Arrays.asList(new Object[]{true, 20}, new Object[]{false, 20}, 
            new Object[]{true, 100000}, new Object[]{false, 100000});
    }

    public TcpChannelTests(boolean secured, int packetSize) throws IOException
    {
        this.secured = secured;
        this.packetSize = packetSize;

        byte[] buffer = new byte[packetSize];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = (byte)i;

        packet = new ByteArray(buffer);
        multiPacket = createMultiPacket(packetSize);
    }

    @Before
    public void setUp() throws Throwable
    {
        int multiplier = DEBUG ? 1000 : 1;
        InetAddress bindAddress = getBindAddress();
        int portRangeStart = 40000;
        int portRangeEnd = 40100;

        dispatchers = new TcpNioDispatcher[COUNT];
        compartments = new ICompartment[COUNT];
        clients = new ITcpChannel[COUNT];
        servers = new ITcpServer[COUNT];
        acceptedChannels = new ITcpChannel[COUNT];

        ITcpSocketChannelFactory channelFactory;
        if (secured)
        {
            SSLContext sslContext = createSslContext();
            channelFactory = new TcpSslSocketChannelFactory(sslContext, true, true);
        }
        else
            channelFactory = new TcpSocketChannelFactory();

        for (int i = 0; i < COUNT; i++)
        {
            TcpNioDispatcher dispatcher;

            dispatcher = new TcpNioDispatcher(3000 * multiplier, 5000 * multiplier, "node" + i);

            ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
            compartmentParameters.name = "node" + i;
            compartmentParameters.dispatchPeriod = 100;
            compartmentParameters.dispatcher = dispatcher;
            compartments[i] = new CompartmentFactory().createCompartment(compartmentParameters);
            
            ITcpServer.Parameters parameters = new ITcpServer.Parameters();
            parameters.bindAddress = bindAddress;
            parameters.portRangeStart = portRangeStart;
            parameters.portRangeEnd = portRangeEnd;
            parameters.channelAcceptor = new ChannelAcceptorMock(i);
            parameters.socketChannelFactory = channelFactory;
            ITcpServer server = dispatcher.createServer(parameters);
            InetSocketAddress address = server.getLocalAddress();
            assertThat(server.isOpened(), is(true));
            assertThat(address.getPort() >= portRangeStart && address.getPort() <= portRangeEnd, is(true));

            servers[i] = server;
            dispatchers[i] = dispatcher;
        }

        for (int i = 0; i < COUNT; i++)
        {
            InetSocketAddress remoteAddress;
            String name;
            if (i < COUNT - 1)
            {
                remoteAddress = servers[i + 1].getLocalAddress();
                name = "node" + (i + 1);
            }
            else
            {
                remoteAddress = servers[0].getLocalAddress();
                name = "node0";
            }

            ITcpChannel.Parameters parameters = new ITcpPacketChannel.Parameters();
            if (i == 1)
            {
                ((ITcpPacketChannel.Parameters)parameters).packetSerializer = new PacketSerializerMock();
                ((ITcpPacketChannel.Parameters)parameters).channelHandshaker = new ChannelHandshakerMock(true);
            }
            if (i == 7)
                ((ITcpPacketChannel.Parameters)parameters).maxPacketSize = 10;

            parameters.channelListeners.add(new ChannelListenerMock(-1));
            parameters.channelReader = new ChannelReaderMock();
            parameters.channelWriter = new ChannelWriterMock();
            parameters.name = name;
            parameters.socketChannelFactory = channelFactory;
            ITcpChannel client = dispatchers[i].createClient(remoteAddress, bindAddress, parameters);

            assertThat(client.isDisconnected(), is(false));
            clients[i] = client;
        }

        for (int i = 0; i < COUNT; i++)
            ((ILifecycle)compartments[i]).start();

        Thread.sleep(2000);

        for (int i = 0; i < COUNT; i++)
            assertThat(clients[i].isConnected(), is(true));

        for (int i = 0; i < COUNT; i++)
            assertThat(acceptedChannels[i].isConnected(), is(true));

    }

    @After
    public void tearDown() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
            ((ILifecycle)compartments[i]).stop();

        for (ITcpServer server : servers)
            assertThat(server.isOpened(), is(false));

        for (ITcpChannel client : clients)
            assertThat(client.isDisconnected(), is(true));
    }

    @Test
    public void testDispatcher() throws Throwable
    {
        Thread.sleep(500);

        // Test connections
        for (ITcpFactory dispatcher : dispatchers)
        {
            Set<SelectionKey> keys = ((TcpNioDispatcher)dispatcher).getSelector().keys();
            assertThat(keys.size(), is(3));
        }

        // Test connection cleanup when disconnected
        acceptedChannels[1].disconnect();

        Thread.sleep(2500);

        assertThat(clients[0].isDisconnected(), is(true));
        assertThat(clients[1].isDisconnected(), is(false));

        // Test connection cleanup when idle
        for (int k = 0; k < 30; k++)
        {
            for (int i = 1; i < COUNT / 2; i++)
            {
                ChannelWriterMock writer = (ChannelWriterMock)((TcpAbstractChannel)clients[i]).getChannelWriter();
                ChannelReaderMock reader = (ChannelReaderMock)((TcpAbstractChannel)clients[i]).getChannelReader();
                ChannelReaderMock acceptedReader = (ChannelReaderMock)((TcpAbstractChannel)acceptedChannels[i]).getChannelReader();
                reader.reset();
                acceptedReader.reset();
                if (i != 1)
                    writer.packet = new TcpPacket(packet);
                else
                    writer.packet = "test";
                writer.reset();

                if (!clients[i].isDisconnected())
                    clients[i].updateWriteStatus();
            }

            Thread.sleep(250);
        }

//        for (int i = 1; i < COUNT / 2; i++)
//            assertThat(clients[i].isDisconnected(), is(false));
        for (int i = COUNT / 2; i < COUNT; i++)
            assertThat(clients[i].isDisconnected(), is(true));

        // Test dispatcher's stop
        ((ILifecycle)compartments[2]).stop();

        assertThat((dispatchers[2]).getSelector().isOpen(), is(false));
        assertThat(clients[2].isDisconnected(), is(true));
        assertThat(servers[2].isOpened(), is(false));

        // Test server close
        servers[4].close();

        ITcpChannel.Parameters parameters = new ITcpPacketChannel.Parameters();

        parameters.channelListeners.add(new ChannelListenerMock(-1));
        parameters.channelReader = new ChannelReaderMock();
        parameters.channelWriter = new ChannelWriterMock();
        parameters.name = "test";

        ITcpChannel client = dispatchers[3].createClient(servers[4].getLocalAddress(), null, parameters);
        Thread.sleep(2000);

        assertThat(client.isConnected(), is(false));
    }

    @Test
    public void testChannelListeners() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            Set<ITcpChannelListener> listeners = ((TcpAbstractChannel)clients[i]).getChannelListeners();
            assertThat(listeners.size(), is(1));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(0));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

            listeners = ((TcpAbstractChannel)acceptedChannels[i]).getChannelListeners();
            assertThat(listeners.size(), is(1));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(0));
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));
        }

        clients[0].disconnect();

        Thread.sleep(200);

        Set<ITcpChannelListener> listeners = ((TcpAbstractChannel)clients[0]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        listeners = ((TcpAbstractChannel)acceptedChannels[1]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        if (secured)
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        clients[1].disconnect();

        Thread.sleep(200);

        listeners = ((TcpAbstractChannel)clients[1]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        listeners = ((TcpAbstractChannel)acceptedChannels[2]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        clients[2].close();

        Thread.sleep(1000);

        listeners = ((TcpAbstractChannel)clients[2]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(0));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(1));

        listeners = ((TcpAbstractChannel)acceptedChannels[3]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        if (secured)
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(1));
        else
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));

        ((ILifecycle)compartments[4]).stop();

        Thread.sleep(200);

        listeners = ((TcpAbstractChannel)clients[4]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        listeners = ((TcpAbstractChannel)acceptedChannels[4]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        listeners = ((TcpAbstractChannel)clients[3]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        if (secured)
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));

        listeners = ((TcpAbstractChannel)acceptedChannels[5]).getChannelListeners();
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getConnected(), is(1));
        if (secured)
            assertThat(((ChannelListenerMock)listeners.iterator().next()).getDisconnected(), is(1));
        assertThat(((ChannelListenerMock)listeners.iterator().next()).getFailed(), is(0));
    }

    @Test
    public void testReadWrite() throws Throwable
    {
        ChannelWriterMock writer = (ChannelWriterMock)((TcpAbstractChannel)clients[0]).getChannelWriter();
        ChannelReaderMock reader = (ChannelReaderMock)((TcpAbstractChannel)acceptedChannels[1]).getChannelReader();

        // Test packet
        reader.reset();
        reader.enableRead(false);
        acceptedChannels[1].updateReadStatus();

        fileNames.clear();
        writer.init(5, null, null, null);
        clients[0].updateWriteStatus();

        Thread.sleep(300);

        assertThat(reader.packets.isEmpty(), is(true));
        reader.enableRead(true);
        acceptedChannels[1].updateReadStatus();

        Thread.sleep(1000);

        assertThat(reader.packets.size(), is(5));
        for (Object received : reader.packets)
        {
            TcpPacket packet = (TcpPacket)received;
            if (packet.getFiles() != null)
            {
                for (File file : packet.getFiles())
                    file.deleteOnExit();
            }
        }
        for (Object received : reader.packets)
            checkPacket((TcpPacket)received, multiPacket);

        for (File file : fileNames)
            assertThat(file.exists(), is(false));
        
        fileNames.clear();
        
        // Test serializer
        writer = (ChannelWriterMock)((TcpAbstractChannel)clients[1]).getChannelWriter();
        reader = (ChannelReaderMock)((TcpAbstractChannel)acceptedChannels[2]).getChannelReader();
        reader.reset();
        reader.enableRead(true);
        acceptedChannels[2].updateReadStatus();

        String message = "Hello world!!!";
        writer.init(5, message, null, null);
        clients[1].updateWriteStatus();

        Thread.sleep(500);

        assertThat(reader.packets.size(), is(5));
        for (Object received : reader.packets)
            assertThat(received, is((Object)message));

        // Test max message size
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                byte[] b = new byte[packet.getLength() + 1];
                System.arraycopy(packet.getBuffer(), packet.getOffset(), b, 0,
                    packet.getLength());
                ((ITcpPacketChannel)clients[7]).write(new TcpPacket(new ByteArray(b)));
            }
        });
        writer = (ChannelWriterMock)((TcpAbstractChannel)clients[8]).getChannelWriter();
        reader = (ChannelReaderMock)((TcpAbstractChannel)acceptedChannels[9]).getChannelReader();
        reader.reset();
        reader.enableRead(true);
        acceptedChannels[9].updateReadStatus();

        writer.init(5, new TcpPacket(packet), null, null);
        clients[8].updateWriteStatus();

        Thread.sleep(300);

        assertThat(reader.packets.isEmpty(), is(true));
    }

    private InetAddress getBindAddress()
    {
        try
        {
            return InetAddress.getLocalHost();
//            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//            while (networkInterfaces.hasMoreElements())
//            {
//                List<InterfaceAddress> interfaceAddresses = networkInterfaces.nextElement().getInterfaceAddresses();
//                for (InterfaceAddress address : interfaceAddresses)
//                {
//                    if (address.getAddress() instanceof Inet4Address && address.getAddress().isLoopbackAddress())
//                        return address.getAddress();
//                }
//            }
        }
        catch (Exception e)
        {
            throw new TcpException(e);
        }
    }

    private SSLContext createSslContext() throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(getClass().getResourceAsStream("keystore.jks"), "testtest".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "testtest".toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private TcpPacket createMultiPacket(int packetSize)
    {
        byte[] buffer;
        List<ByteArray> buffers = new ArrayList<ByteArray>();
        List<File> files = new ArrayList<File>();
        for (int k = 0; k < 10; k++)
        {
            buffer = new byte[packetSize];
            for (int i = 0; i < buffer.length; i++)
                buffer[i] = (byte)(i + k);
            
            buffers.add(new ByteArray(buffer));
     
            try
            {
                File fileName = File.createTempFile("exa-test", ".tmp");
                fileNames.add(fileName);
                fileName.deleteOnExit();
                RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                file.write(buffer);
                file.close();
                files.add(fileName);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        return new TcpPacket(buffers, files, null);
    }

    private void checkPacket(TcpPacket packet, TcpPacket ethalon) throws IOException
    {
        ByteArray buf1 = combine(packet.getBuffers());
        ByteArray buf2 = combine(ethalon.getBuffers());
        assertThat(buf1, is(buf2));
        assertThat(packet.getSize(), is(ethalon.getSize())); 
        assertThat(packet.getFiles().size(), is(ethalon.getFiles().size()));
        for (int i = 0; i < packet.getFiles().size(); i++)
        {
            File file = packet.getFiles().get(i);
            File ethalonFile = ethalon.getFiles().get(i);
            IOs.equals(new FileInputStream(file), new FileInputStream(ethalonFile));
        }
    }
    
    private ByteArray combine(List<ByteArray> buffers)
    {
        ByteOutputStream stream = new ByteOutputStream();
        for (ByteArray buffer : buffers)
            stream.write(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        
        return new ByteArray(stream.getBuffer(), 0, stream.getLength());
    }

    private class ChannelAcceptorMock implements ITcpChannelAcceptor
    {
        private final int i;

        public ChannelAcceptorMock(int i)
        {
            this.i = i;
        }

        @Override
        public ITcpChannel.Parameters accept(InetSocketAddress remoteAddress)
        {
            ITcpChannel.Parameters parameters = new ITcpPacketChannel.Parameters();

            if (i == 2)
            {
                ((ITcpPacketChannel.Parameters)parameters).packetSerializer = new PacketSerializerMock();
                ((ITcpPacketChannel.Parameters)parameters).channelHandshaker = new ChannelHandshakerMock(false);
            }
            if (i == 9)
                ((ITcpPacketChannel.Parameters)parameters).maxPacketSize = 10;

            parameters.channelListeners.add(new ChannelListenerMock(i));
            parameters.channelReader = new ChannelReaderMock();
            parameters.channelWriter = new ChannelWriterMock();
            if (i != 0)
                parameters.name = "node" + (i - 1);
            else
                parameters.name = "node" + (COUNT - 1);

            return parameters;
        }
    }

    private class ChannelListenerMock implements ITcpChannelListener
    {
        private int connected;
        private int disconnected;
        private int failed;
        private final int i;

        public ChannelListenerMock(int i)
        {
            this.i = i;
        }

        public synchronized int getConnected()
        {
            return connected;
        }

        public synchronized int getDisconnected()
        {
            return disconnected;
        }

        public synchronized int getFailed()
        {
            return failed;
        }

        @Override
        public synchronized void onConnected(ITcpChannel channel)
        {
            connected++;

            if (i >= 0)
                acceptedChannels[i] = channel;
        }

        @Override
        public synchronized void onDisconnected(ITcpChannel channel)
        {
            disconnected++;
        }

        @Override
        public synchronized void onFailed(ITcpChannel channel)
        {
            failed++;
        }
    }

    private class ChannelReaderMock implements ITcpChannelReader
    {
        boolean canRead = true;// !blocked;
        List<Object> packets = new ArrayList<Object>();

        public synchronized void enableRead(boolean enabled)
        {
            canRead = enabled;
        }

        public synchronized void reset()
        {
            canRead = true;
            packets.clear();
        }

        @Override
        public synchronized boolean canRead(ITcpChannel channel)
        {
            return canRead;
        }

        @Override
        public void onRead(ITcpChannel channel)
        {
            ITcpPacketChannel packetChannel = (ITcpPacketChannel)channel;
            Object packet = packetChannel.read();
            if (packet != null)
                packets.add(packet);
        }
    }

    private class ChannelWriterMock implements ITcpChannelWriter
    {
        int writeCount;
        Object packet;
        ByteBuffer buffer;
        ByteBuffer[] buffers;

        public synchronized void init(int writeCount, Object packet, ByteBuffer buffer, ByteBuffer[] buffers)
        {
            this.writeCount = writeCount;
            this.buffer = buffer;
            this.buffers = buffers;
            this.packet = packet;
        }

        public synchronized void reset()
        {
            writeCount = 5;
            buffers = null;
        }

        @Override
        public synchronized void onWrite(ITcpChannel channel)
        {
            if (writeCount == 0)
                return;

            ITcpPacketChannel packetChannel = (ITcpPacketChannel)channel;
            if (packetChannel.write(packet != null ? packet : createMultiPacket(packetSize)))
                writeCount--;
        }

        @Override
        public synchronized boolean canWrite(ITcpChannel channel)
        {
            if (writeCount > 0 || (buffer != null && buffer.hasRemaining()))
                return true;

            if (buffers != null)
            {
                for (ByteBuffer buf : buffers)
                {
                    if (buf.hasRemaining())
                        return true;
                }
            }

            return false;
        }
    }

    private static class ChannelHandshakerMock implements ITcpChannelHandshaker<String>
    {
        enum State
        {
            NEED_READ, NEED_WRITE, CONNECTED, DISCONNECTED
        }

        private final boolean client;
        private State state;
        private String lastResult;

        public ChannelHandshakerMock(boolean client)
        {
            this.client = client;
            if (client)
                state = State.NEED_WRITE;
            else
                state = State.NEED_READ;
        }

        @Override
        public boolean handshake(ITcpPacketChannel<String> channel)
        {
            switch (state)
            {
            case NEED_READ:
                {
                    if (client)
                    {
                        String result = channel.read();
                        if (result == null)
                            return false;

                        if (lastResult == null)
                        {
                            Assert.assertTrue(result.equals("handshake2"));

                            lastResult = result;
                            state = State.NEED_WRITE;

                            return handshake(channel);
                        }
                        else if (lastResult.equals("handshake2"))
                        {
                            Assert.assertTrue(result.equals("handshake4"));

                            lastResult = null;
                            state = State.CONNECTED;

                            return true;
                        }
                        else
                            Assert.assertTrue(false);
                    }
                    else
                    {
                        String result = channel.read();
                        if (result == null)
                            return false;

                        if (lastResult == null)
                        {
                            Assert.assertTrue(result.equals("handshake1"));

                            lastResult = result;
                            state = State.NEED_WRITE;

                            return handshake(channel);
                        }
                        else if (lastResult.equals("handshake1"))
                        {
                            Assert.assertTrue(result.equals("handshake3"));

                            lastResult = result;
                            state = State.NEED_WRITE;

                            return handshake(channel);
                        }
                        else
                            Assert.assertTrue(false);
                    }
                }
            case NEED_WRITE:
                {
                    if (client)
                    {
                        if (lastResult == null)
                        {
                            if (channel.write("handshake1"))
                                state = State.NEED_READ;
                            return false;
                        }
                        else if (lastResult.equals("handshake2"))
                        {
                            if (channel.write("handshake3"))
                                state = State.NEED_READ;

                            return false;
                        }
                        else
                            Assert.assertTrue(false);

                    }
                    else
                    {
                        if (lastResult.equals("handshake1"))
                        {
                            if (channel.write("handshake2"))
                                state = State.NEED_READ;
                            return false;
                        }
                        else if (lastResult.equals("handshake3"))
                        {
                            if (channel.write("handshake4"))
                            {
                                lastResult = null;
                                state = State.CONNECTED;
                                return true;
                            }
                        }
                        else
                            Assert.assertTrue(false);
                    }
                }
            case CONNECTED:
                return true;
            default:
                Assert.assertTrue(false);

            }

            return false;
        }

        @Override
        public boolean disconnect(ITcpPacketChannel<String> channel)
        {
            if (state == State.DISCONNECTED)
                return true;

            String result = channel.read();
            if (result == null)
            {
                if (channel.write("disconnect"))
                {
                    state = State.DISCONNECTED;
                    return true;
                }

                return false;
            }

            Assert.assertTrue(result.equals("disconnect"));
            state = State.DISCONNECTED;
            return true;
        }

        @Override
        public boolean canDisconnect(String packet)
        {
            return packet.startsWith("disconnect");
        }
    }

    private static class PacketSerializerMock implements ITcpPacketSerializer
    {
        @Override
        public TcpPacket serialize(Object packet)
        {
            byte[] bytes = ((String)packet).getBytes();
            byte[] data = new byte[bytes.length];
            System.arraycopy(bytes, 0, data, 0, bytes.length);
            return new TcpPacket(new ByteArray(data));
        }

        @Override
        public Object deserialize(TcpPacket packet)
        {
            assertThat(packet.getBuffers().size(), is(1));
            ByteArray buffer = packet.getBuffers().get(0);
            return new String(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        }
    }
}