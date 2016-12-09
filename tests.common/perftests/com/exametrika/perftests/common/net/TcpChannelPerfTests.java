/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.net;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannel.Parameters;
import com.exametrika.common.net.ITcpChannelAcceptor;
import com.exametrika.common.net.ITcpChannelListener;
import com.exametrika.common.net.ITcpChannelReader;
import com.exametrika.common.net.ITcpChannelWriter;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.TcpAbstractChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;
import com.exametrika.common.net.nio.ssl.TcpSslSocketChannelFactory;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.tasks.impl.Daemon;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.IOs;



/**
 * The {@link TcpChannelPerfTests} are performance tests for nio framework.
 * 
 * @author Medvedev-A
 */
public class TcpChannelPerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TcpChannelPerfTests.class);
    private static final int SMALL_COUNT = 1000000;
    private static final int MEDIUM_COUNT = 100000;
    private static final int LARGE_COUNT = 10000;
    private static final int VERY_LARGE_COUNT = 100;
    private static final int SMALL_SIZE = 10;
    private static final int MEDIUM_SIZE = 1000;
    private static final int LARGE_SIZE = 100000;
    private static final int VERY_LARGE_SIZE = 10000000;
    private ICompartment compartment1;
    private ICompartment compartment2;
    private TcpNioDispatcher dispatcher1;
    private TcpNioDispatcher dispatcher2;
    private ITcpServer server;
    private ITcpChannel client;
    private ITcpChannel acceptedChannel;
    private boolean received;
    private boolean canSend;
    private Sequencer sequencer = new Sequencer();

    @Test
    public void testChannel() throws Throwable
    {
        
        logger.log(LogLevel.INFO, messages.separator());
        logger.log(LogLevel.INFO, messages.parameters(SMALL_COUNT, SMALL_SIZE, MEDIUM_COUNT, MEDIUM_SIZE, LARGE_COUNT, LARGE_SIZE));

//        testCaseChannel(false, Type.SMALL, PacketType.SINGLE_BUFFER);
//        testCaseChannel(true, Type.SMALL, PacketType.SINGLE_BUFFER);
//        testCaseChannel(false, Type.SMALL, PacketType.MULTI_BUFFER);
//        testCaseChannel(true, Type.SMALL, PacketType.MULTI_BUFFER);
//        testCaseChannel(false, Type.SMALL, PacketType.FILE);
//        testCaseChannel(true, Type.SMALL, PacketType.FILE);
//        
//        testCaseChannel(false, Type.MEDIUM, PacketType.SINGLE_BUFFER);
//        testCaseChannel(true, Type.MEDIUM, PacketType.SINGLE_BUFFER);
//        testCaseChannel(false, Type.MEDIUM, PacketType.MULTI_BUFFER);
//        testCaseChannel(true, Type.MEDIUM, PacketType.MULTI_BUFFER);
//        testCaseChannel(false, Type.MEDIUM, PacketType.FILE);
//        testCaseChannel(true, Type.MEDIUM, PacketType.FILE);
//        
//        testCaseChannel(false, Type.LARGE, PacketType.SINGLE_BUFFER);
//        testCaseChannel(true, Type.LARGE, PacketType.SINGLE_BUFFER);
//        testCaseChannel(false, Type.LARGE, PacketType.MULTI_BUFFER);
//        testCaseChannel(true, Type.LARGE, PacketType.MULTI_BUFFER);
//        testCaseChannel(false, Type.LARGE, PacketType.FILE);
//        testCaseChannel(true, Type.LARGE, PacketType.FILE);
        
        testCaseChannel(false, Type.VERY_LARGE, PacketType.SINGLE_BUFFER);
        testCaseChannel(true, Type.VERY_LARGE, PacketType.SINGLE_BUFFER);
        testCaseChannel(false, Type.VERY_LARGE, PacketType.MULTI_BUFFER);
        testCaseChannel(true, Type.VERY_LARGE, PacketType.MULTI_BUFFER);
        testCaseChannel(false, Type.VERY_LARGE, PacketType.FILE);
        testCaseChannel(true, Type.VERY_LARGE, PacketType.FILE);
    }
    
    @Test
    public void testSocket() throws Throwable
    {
        logger.log(LogLevel.INFO, messages.separator());
        testCaseSocket(false, SMALL_COUNT, SMALL_SIZE, false);
        testCaseSocket(true, SMALL_COUNT, SMALL_SIZE, false);
        testCaseSocket(false, SMALL_COUNT, SMALL_SIZE, true);
        testCaseSocket(true, SMALL_COUNT, SMALL_SIZE, true);
        testCaseSocket(false, MEDIUM_COUNT, MEDIUM_SIZE, false);
        testCaseSocket(true, MEDIUM_COUNT, MEDIUM_SIZE, false);
        testCaseSocket(false, MEDIUM_COUNT, MEDIUM_SIZE, true);
        testCaseSocket(true, MEDIUM_COUNT, MEDIUM_SIZE, true);
        testCaseSocket(false, LARGE_COUNT, LARGE_SIZE, false);
        testCaseSocket(true, LARGE_COUNT, LARGE_SIZE, false);
        testCaseSocket(false, LARGE_COUNT, LARGE_SIZE, true);
        testCaseSocket(true, LARGE_COUNT, LARGE_SIZE, true);
    }
    
    private void testCaseSocket(boolean secured, final int count, final int size, final boolean buffered) throws Throwable
    {
        SocketFactory socketFactory;
        ServerSocketFactory serverSocketFactory;
        if (secured)
        {
            SSLContext sslContext = createSslContext();
            socketFactory = sslContext.getSocketFactory();
            serverSocketFactory = sslContext.getServerSocketFactory();
        }
        else
        {
            socketFactory = SocketFactory.getDefault();
            serverSocketFactory = ServerSocketFactory.getDefault();
        }
        ServerSocket serverSocket = serverSocketFactory.createServerSocket(0);
        final Socket client = socketFactory.createSocket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
        final Socket accepted = serverSocket.accept();
        
        final byte[] buffer = new byte[size];
        fillBuffer(buffer);
        Daemon daemon1 = new Daemon(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    sendWait();
                    
                    OutputStream stream;
                    if (buffered)
                        stream = new BufferedOutputStream(client.getOutputStream());
                    else
                        stream = client.getOutputStream();
                    for (int i = 0; i < count; i++)
                        stream.write(buffer);
                    
                    stream.close();
                    client.close();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        
        final byte[] buffer2 = new byte[size];
        Daemon daemon2 = new Daemon(new Runnable()
        {
            int total; 
            
            @Override
            public void run()
            {
                try
                {
                    InputStream stream;
                    if (buffered)
                        stream = new BufferedInputStream(accepted.getInputStream());
                    else
                        stream = accepted.getInputStream();
                    while (true)
                    {
                        int n = stream.read(buffer2);
                        if (n == -1)
                            break;
                        
                        total += n;
                    }
                    
                    stream.close();
                    accepted.close();
                    if (total == count * size)
                        receiveNotify();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        
        daemon2.start();
        daemon1.start();
        
        Thread.sleep(100);

        logger.log(LogLevel.INFO, messages.smallSeparator());
        logger.log(LogLevel.INFO, messages.testSocket(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                sendNotify();
                receiveWait();
            }
        }, 1, 0), secured, count, size, buffered));

        daemon2.stop();
        daemon1.stop();
        
        serverSocket.close();
    }

    private void testCaseChannel(boolean secured, Type type, PacketType packetType) throws Throwable
    {
        createChannels(secured, type, packetType);
        logger.log(LogLevel.INFO, messages.smallSeparator());
        logger.log(LogLevel.INFO, messages.testChannel(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                doTest();
            }
        }, 1, 0), secured, type, packetType));
    }
    
    private void doTest()
    {
        try
        {
            received = false;
            ((ChannelWriter)((TcpAbstractChannel)client).getChannelWriter()).canWrite = true;
            client.updateWriteStatus();
            receiveWait();
        }
        finally
        {
            destroyChannels();
        }
    }
    
    private synchronized void receiveWait()
    {
        while (!received)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    
    private synchronized void receiveNotify()
    {
        received = true;
        notify();
    }

    private synchronized void sendWait()
    {
        canSend = false;
        while (!canSend)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    
    private synchronized void sendNotify()
    {
        canSend = true;
        notify();
    }

    private enum Type
    {
        VERY_LARGE,
        LARGE,
        MEDIUM,
        SMALL
    }
    
    private enum PacketType
    {
        SINGLE_BUFFER,
        MULTI_BUFFER,
        FILE
    }
    
    private ITcpChannel.Parameters createParameters(Type type, PacketType packetType, boolean client) throws Throwable
    {
        ITcpChannel.Parameters parameters;
        ITcpPacketChannel.Parameters packetParameters = new ITcpPacketChannel.Parameters();
        ChannelReader channelReader = new ChannelReader();
        ChannelWriter channelWriter = new ChannelWriter();
        
        int bufferCount = 0;
        int bufferSize = 0;
        int fileCount = 0;
        int fileSize = 0;
        int count = 0;
        
        int divider = 1;
        switch (packetType)
        {
        case SINGLE_BUFFER:
            bufferCount = 1;
            fileCount = 0;
            divider = 1;
            break;
        case MULTI_BUFFER:
            bufferCount = 10;
            fileCount = 0;
            divider = 10;
            break;
        case FILE:
            bufferCount = 1;
            fileCount = 1;
            divider = 10;
            break;
        default:
            Assert.error();
        }
        
        switch (type)
        {
        case VERY_LARGE:
            bufferSize = VERY_LARGE_SIZE / divider;
            fileSize = VERY_LARGE_SIZE;
            count = VERY_LARGE_COUNT;
            break;
        case LARGE:
            bufferSize = LARGE_SIZE / divider;
            fileSize = LARGE_SIZE;
            count = LARGE_COUNT;
            break;
        case MEDIUM:
            bufferSize = MEDIUM_SIZE / divider;
            fileSize = MEDIUM_SIZE;
            count = MEDIUM_COUNT;
            break;
        case SMALL:
            bufferSize = SMALL_SIZE / divider;
            fileSize = SMALL_SIZE;
            count = SMALL_COUNT;
            break;
        }
        
        channelWriter.packet = createPacket(bufferCount, bufferSize, fileCount, fileSize);
        channelReader.ethalonPacket = createPacket(bufferCount, bufferSize, fileCount, fileSize);
        channelWriter.writeCount = count;
        channelReader.readCount = count;
        
        packetParameters.channelReader = channelReader;
        packetParameters.channelWriter = channelWriter;
        packetParameters.channelListeners = Collections.asSet((ITcpChannelListener)new ChannelListener(client));
        packetParameters.disableFileDeletion = true;
        
        parameters = packetParameters;
        
        if (client)
            parameters.name = "node2";
        else
            parameters.name = "node1";
        
//        int rate;
//        switch (type)
//        {
//        case LARGE:
//            rate = 100000000;
//            break;
//        case MEDIUM:
//            rate = 1000000;
//            break;
//        case SMALL:
//            rate = 10000;
//            break;
//        default:
//            rate = 0;
//        }
        //parameters.rateController = new TcpRateController(true, rate / 2, rate, true, rate / 2, rate, false, 100, 10);
        return parameters;
    }
    
    private void fillBuffer(byte[] buffer)
    {
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = (byte)i;
    }
    
    private TcpPacket createPacket(int bufferCount, int bufferSize, int fileCount, int fileSize) throws IOException
    {
        List<ByteArray> buffers = new ArrayList<ByteArray>();
        for (int k = 0; k < bufferCount; k++)
        {
            byte[] buffer = new byte[bufferSize];
            for (int i = 0; i < bufferSize; i++)
                buffer[i] = (byte)i;
            
            buffers.add(new ByteArray(buffer));
        }
        
        List<File> files = null;
        if (fileCount > 0)
        {
            files = new ArrayList<File>();
            for (int k = 0; k < fileCount; k++)
            {
                byte[] buffer = new byte[fileSize];
                for (int i = 0; i < fileSize; i++)
                    buffer[i] = (byte)i;
                
                File fileName = File.createTempFile("exa-test", ".tmp");
                fileName.deleteOnExit();
                RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                file.write(buffer);
                file.close();
                files.add(fileName);
            }
        }
        
        return new TcpPacket(buffers, files, null);
    }
    
    private void createChannels(boolean secured, Type type, PacketType packetType) throws Throwable
    {
        ITcpSocketChannelFactory socketChannelFactory;
        if (secured)
            socketChannelFactory = new TcpSslSocketChannelFactory(createSslContext(), true, true);
        else
            socketChannelFactory = new TcpSocketChannelFactory();
        
        dispatcher1 = new TcpNioDispatcher(0, 0, "node" + 1);
        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.name = "node" + 1;
        compartmentParameters.dispatchPeriod = 100;
        compartmentParameters.dispatcher = dispatcher1;
        compartment1 = new CompartmentFactory().createCompartment(compartmentParameters);
        
        dispatcher2 = new TcpNioDispatcher(0, 0, "node" + 2);
        compartmentParameters.name = "node" + 2;
        compartmentParameters.dispatchPeriod = 100;
        compartmentParameters.dispatcher = dispatcher2;
        compartment2 = new CompartmentFactory().createCompartment(compartmentParameters);
        
        ITcpServer.Parameters serverParameters = new ITcpServer.Parameters();
        serverParameters.socketChannelFactory = socketChannelFactory;
        serverParameters.channelAcceptor = new ChannelAcceptor(createParameters(type, packetType, false));

        server = dispatcher2.createServer(serverParameters);
        
        ITcpChannel.Parameters channelParameters = createParameters(type, packetType, true);
        channelParameters.socketChannelFactory = socketChannelFactory;
        client = dispatcher1.createClient(server.getLocalAddress(), null, channelParameters);
        
        sequencer.createBarrier(3);
        
        ((ILifecycle)compartment1).start();
        ((ILifecycle)compartment2).start();
        
        sequencer.waitBarrier();
        
        assertThat(client.isConnected(), is(true));
        assertThat(acceptedChannel.isConnected(), is(true));
    }
    
    private void destroyChannels()
    {
        if (compartment1 != null)
            ((ILifecycle)compartment1).stop();
        if (compartment2 != null)
            ((ILifecycle)compartment2).stop();
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

    private class ChannelReader implements ITcpChannelReader
    {
        int readCount;
        TcpPacket ethalonPacket;
        int index;
        
        @Override
        public void onRead(ITcpChannel channel)
        {
            if (index == readCount)
                return;
            
            ITcpPacketChannel packetChannel = (ITcpPacketChannel)channel;
            TcpPacket packet = (TcpPacket)packetChannel.read();
            if (packet != null)
            {
                try
                {
                    checkPacket(packet, ethalonPacket);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                index++;
            }
            
            if (index == readCount)
            {
                if (ethalonPacket != null && ethalonPacket.getFiles() != null)
                {
                    for (int i = 0; i < ethalonPacket.getFiles().size(); i++)
                    {
                        File ethalonFile = ethalonPacket.getFiles().get(i);
                        ethalonFile.delete();
                    }
                }
                receiveNotify();
            }
        }
        
        @Override
        public boolean canRead(ITcpChannel channel)
        {
            return true;
        }
        
        private void checkPacket(TcpPacket packet, TcpPacket ethalon) throws IOException
        {
            ByteArray buf1 = combine(packet.getBuffers());
            ByteArray buf2 = combine(ethalon.getBuffers());
            assertThat(buf1, is(buf2));
            assertThat(packet.getSize(), is(ethalon.getSize())); 
            
            assertThat((packet.getFiles() == null), is(ethalon.getFiles() == null));
                
            if (packet.getFiles() != null)
            {
                assertThat(packet.getFiles().size(), is(ethalon.getFiles().size()));
                for (int i = 0; i < packet.getFiles().size(); i++)
                {
                    File file = packet.getFiles().get(i);
                    File ethalonFile = ethalon.getFiles().get(i);
                    FileInputStream stream1 = null;
                    FileInputStream stream2 = null;
                    try
                    {
                        stream1 = new FileInputStream(file);
                        stream2 = new FileInputStream(ethalonFile);
                        IOs.equals(stream1, stream2);
                    }
                    finally
                    {
                        IOs.close(stream1);
                        IOs.close(stream2);
                    }
                    
                    file.delete();
                }
            }
        }
        
        private ByteArray combine(List<ByteArray> buffers)
        {
            ByteOutputStream stream = new ByteOutputStream();
            for (ByteArray buffer : buffers)
                stream.write(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            
            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }
    
    private class ChannelWriter implements ITcpChannelWriter
    {
        volatile boolean canWrite;
        int writeCount;
        TcpPacket packet;
        ByteBuffer buffer;

        @Override
        public void onWrite(ITcpChannel channel)
        {
            if (!canWrite)
                return;
            
            if (writeCount == 0)
                return;
            
            ITcpPacketChannel packetChannel = (ITcpPacketChannel)channel;
            if (packetChannel.write(packet))
                writeCount--;
        }
        
        @Override
        public boolean canWrite(ITcpChannel channel)
        {
            if (!canWrite)
                return false;
            
            if (writeCount > 0)
                return true;
            
            if (buffer != null)
                return buffer.hasRemaining();
            
            return false;
        }
    }
    
    private class ChannelAcceptor implements ITcpChannelAcceptor
    {
        private final Parameters parameters;
        
        public ChannelAcceptor(ITcpChannel.Parameters parameters)
        {
            this.parameters = parameters;
        }
        
        @Override
        public ITcpChannel.Parameters accept(InetSocketAddress remoteAddress)
        {
            return parameters;
        }
    }
    
    private class ChannelListener implements ITcpChannelListener
    {
        private final boolean client;

        public ChannelListener(boolean client)
        {
            this.client = client;
       }
        @Override
        public void onConnected(ITcpChannel channel)
        {
            if (!client)
                acceptedChannel = channel;
            
            sequencer.waitBarrier();
        }

        @Override
        public void onDisconnected(ITcpChannel channel)
        {
        }

        @Override
        public void onFailed(ITcpChannel channel)
        {
        }
    }

    private interface IMessages
    {
        @DefaultMessage("====================================================================")
        ILocalizedMessage separator();
        @DefaultMessage("--------------------------------------------------------------------")
        ILocalizedMessage smallSeparator();
        @DefaultMessage("small count:{0}, small size: {1}, medium count:{2}, medium size:{3}, large count:{4}, large size:{5}")
        ILocalizedMessage parameters(int smallCount, int smallSize, int mediumCount, int mediumSize, int largeCount, int largeSize);
        @DefaultMessage("[TCP channel read/write test (secured:{1}, type:{2}, packet type: {3})] {0}.")
        ILocalizedMessage testChannel(Benchmark benchmark, boolean secured, Type type, PacketType packetType);
        @DefaultMessage("[TCP socket read/write test (secured: {1}, count:{2}, size:{3}, buffered:{4})] {0}.")
        ILocalizedMessage testSocket(Benchmark benchmark, boolean secured, int count, int size, boolean buffered);
    }
}
