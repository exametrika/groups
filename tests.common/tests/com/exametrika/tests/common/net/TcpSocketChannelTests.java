/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.net;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exametrika.common.net.nio.socket.ITcpSelector;
import com.exametrika.common.net.nio.socket.ITcpServerSocketChannel;
import com.exametrika.common.net.nio.socket.ITcpSocketChannel;
import com.exametrika.common.net.nio.socket.TcpSelector;
import com.exametrika.common.net.nio.socket.TcpServerSocketChannel;
import com.exametrika.common.net.nio.socket.TcpSocketChannel;
import com.exametrika.common.net.nio.ssl.TcpSslServerSocketChannel;
import com.exametrika.common.net.nio.ssl.TcpSslSocketChannel;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IOs;


/**
 * The {@link TcpSocketChannelTests} are tests for {@link TcpSslServerSocketChannel} and {@link TcpSslSocketChannel}.
 * 
 * @see TcpSslServerSocketChannel
 * @see TcpSslSocketChannel
 * @author Medvedev-A
 */
@RunWith(Parameterized.class)
public class TcpSocketChannelTests
{
    private static final int MESSAGE_SIZE = 100000; 
    private final boolean secured;
    private ITcpServerSocketChannel serverChannel;
    private ITcpSocketChannel clientChannel;
    private ITcpSelector serverSelector;
    private ITcpSocketChannel acceptedChannel;
    private ITcpSelector clientSelector;
    private SelectionKey clientKey;
    private SelectionKey serverKey;
    private ByteArray message;
    
    @Parameters
    public static Collection<?> parameters()
    {
        return Arrays.asList(new Object[]{true}, new Object[]{false});
    }

    public TcpSocketChannelTests(boolean secured)
    {
        this.secured = secured;
    }

    @Before
    public void setUp() throws Throwable
    {
        SSLContext sslContext;
        if (secured)
            sslContext = createSslContext();
        else
            sslContext = null;
        
        serverSelector = new TcpSelector();
        clientSelector = new TcpSelector();
        
        if (secured)
            serverChannel = new TcpSslServerSocketChannel(sslContext, true, true);
        else
            serverChannel = new TcpServerSocketChannel();
        
        serverChannel.configureBlocking(false);
        serverChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
        InetSocketAddress address = new InetSocketAddress("localhost", 47474); 
        serverChannel.socket().bind(address);
        
        if (secured)
            clientChannel = new TcpSslSocketChannel(address, sslContext);
        else
            clientChannel = new TcpSocketChannel(address);
        
        assertThat(clientChannel.isConnected(), is(false));
        
        clientChannel.configureBlocking(false);
        clientKey = clientChannel.register(clientSelector, SelectionKey.OP_CONNECT);
        
        byte[] buffer = new byte[MESSAGE_SIZE];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = (byte)i;
        
        message = new ByteArray(buffer);
    }
    
    @After
    public void tearDown() throws Throwable
    {
        IOs.close(acceptedChannel);
        IOs.close(serverChannel);
        IOs.close(clientChannel);
        IOs.close(serverSelector);
        IOs.close(clientChannel);
        
        assertThat(acceptedChannel.isConnected(), is(false));
        assertThat(clientChannel.isConnected(), is(false));
    }
    
    @Test
    public void testChannel() throws Throwable
    {
        wait(serverSelector, SelectionKey.OP_ACCEPT);
        
        acceptedChannel = serverChannel.accept();
        assertThat(acceptedChannel.isConnected(), is(false));
        acceptedChannel.configureBlocking(false);
        serverKey = acceptedChannel.register(serverSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        
        wait(clientSelector, SelectionKey.OP_CONNECT);
        clientChannel.finishConnect();
        
        clientKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        
        wait(clientSelector, SelectionKey.OP_WRITE);

        while (true)
        {
            boolean clientHandshake = clientChannel.handshake();
            boolean serverHandshake = acceptedChannel.handshake();
            if (clientHandshake && serverHandshake)
                break;
            
            Thread.sleep(10);
        }
        
        assertThat(clientChannel.isConnected(), is(true));
        assertThat(acceptedChannel.isConnected(), is(true));

        clientKey.interestOps(SelectionKey.OP_WRITE);
        serverKey.interestOps(SelectionKey.OP_READ);
        
        ByteBuffer outBuffer = ByteBuffer.wrap(message.getBuffer(), message.getOffset(), message.getLength());
        ByteBuffer inBuffer = ByteBuffer.allocate(outBuffer.limit());
        
        while (inBuffer.remaining() > 0)
        {
            clientSelector.select(100);
            if (!clientSelector.selectedKeys().isEmpty())
            {
                if ((clientKey.readyOps() & SelectionKey.OP_WRITE) != 0 && outBuffer.remaining() > 0)
                    clientChannel.write(outBuffer);
                
                if (!clientChannel.hasWriteData())
                    clientSelector.selectedKeys().clear();
            }
            
            serverSelector.select(100);
            if (!serverSelector.selectedKeys().isEmpty())
            {
                if ((serverKey.readyOps() & SelectionKey.OP_READ) != 0 && inBuffer.remaining() > 0)
                    acceptedChannel.read(inBuffer);
                
                if (!acceptedChannel.hasReadData())
                    serverSelector.selectedKeys().clear();
            }
        }

        ByteArray message = new ByteArray(inBuffer.array(), inBuffer.arrayOffset(), inBuffer.capacity());
        assertThat(message, is(this.message));
        
        ByteBuffer[] outBuffers = new ByteBuffer[4];
        for (int i = 0; i < outBuffers.length; i++)
        {
            outBuffers[i] = ByteBuffer.wrap(message.getBuffer(), message.getOffset() + (message.getLength() / outBuffers.length) * i, 
                message.getLength() / outBuffers.length);
        }
        
        ByteBuffer[] inBuffers = new ByteBuffer[2];
        for (int i = 0; i < inBuffers.length; i++)
            inBuffers[i] = ByteBuffer.allocate(message.getLength() / inBuffers.length);

        while (true)
        {
            boolean completed = true;
            for (ByteBuffer buffer : inBuffers)
            {
                if (buffer.hasRemaining())
                {
                    completed = false;
                    break;
                }
            }
            
            if (completed)
                break;
            
            clientSelector.select(100);
            if (!clientSelector.selectedKeys().isEmpty())
            {
                if ((clientKey.readyOps() & SelectionKey.OP_WRITE) != 0)
                    clientChannel.write(outBuffers);
                
                if (!clientChannel.hasWriteData())
                    clientSelector.selectedKeys().clear();
            }
            
            serverSelector.select(100);
            if (!serverSelector.selectedKeys().isEmpty())
            {
                if ((serverKey.readyOps() & SelectionKey.OP_READ) != 0)
                    acceptedChannel.read(inBuffers);
                
                if (!acceptedChannel.hasReadData())
                    serverSelector.selectedKeys().clear();
            }
        }
        
        byte[] buffer = new byte[message.getLength()];
        int offset = 0;
        for (ByteBuffer in : inBuffers)
        {
            in.flip();
            in = in.slice();
            System.arraycopy(in.array(), in.arrayOffset(), buffer, offset, in.limit());
            offset += in.limit();
        }
        
        assertThat(new ByteArray(buffer), is(message));
        
        clientKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        serverKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        
        while (true)
        {
            boolean clientShutdown = clientChannel.disconnect();
            boolean serverShutdown = acceptedChannel.disconnect();
            if (clientShutdown && serverShutdown)
                break;
            
            Thread.sleep(10);
        }
        
        assertThat(clientChannel.isConnected(), is(false));
        assertThat(acceptedChannel.isConnected(), is(false));
    }
    
    private boolean wait(ITcpSelector selector, int ops, int timeout) throws Throwable
    {
        boolean found = false;
        
        while (!found)
        {
            if (selector.select(timeout) == 0)
                return false;
    
            Set<SelectionKey> keys = selector.selectedKeys();
            for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); )
            {
                SelectionKey key = it.next();
                if ((key.readyOps() & ops) != 0)
                {
                    found = true;
                    break;
                }
            }
            
            keys.clear();
        }
        
        return true;
    }
    
    private void wait(ITcpSelector selector, int ops) throws Throwable
    {
        wait(selector, ops, 0);
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
}