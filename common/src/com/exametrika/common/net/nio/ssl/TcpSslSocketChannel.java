/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLSession;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.nio.socket.TcpSocketChannel;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpSslSocketChannel} represents a SSL client socket channel implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpSslSocketChannel extends TcpSocketChannel
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(TcpSslSocketChannel.class);
    private final SSLEngine sslEngine;
    private final ByteBuffer inputNetBuffer;
    private final ByteBuffer inputAppBuffer;
    private final ByteBuffer outputNetBuffer;
    private final ByteBuffer emptyBuffer;
    private SSLEngineResult.HandshakeStatus handshakeStatus;

    public TcpSslSocketChannel(SSLContext sslContext) throws IOException
    {
        this(SocketChannel.open(), sslContext.createSSLEngine(), true, State.NOT_CONNECTED);
    }

    public TcpSslSocketChannel(InetSocketAddress remoteSocketAddress, SSLContext sslContext) throws IOException
    {
        this(SocketChannel.open(remoteSocketAddress), 
            sslContext.createSSLEngine(remoteSocketAddress.getAddress().getCanonicalHostName(), remoteSocketAddress.getPort()), true,
            State.CONNECTING);
    }

    @Override
    public boolean handshake() throws IOException
    {
        if (state != State.CONNECTED)
        {
            if (handshakeStatus == null)
            {
                sslEngine.beginHandshake();
                handshakeStatus = sslEngine.getHandshakeStatus();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.beginHandshake());
            }

            if (!flush())
                return false;

            while (true)
            {
                switch (handshakeStatus)
                {
                case FINISHED:
                    state = State.CONNECTED;
                    inputAppBuffer.limit(0);
                    inputNetBuffer.limit(0);
                    outputNetBuffer.limit(0);
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                    {
                        SSLSession session = sslEngine.getSession();
                        logger.log(LogLevel.DEBUG, marker, messages.connected(session.getCipherSuite()));
                    }
                    return true;
                case NEED_TASK:
                    handshakeStatus = runTasks();
                    return false;
                case NEED_UNWRAP:
                    unwrap();
                    return false;
                case NEED_WRAP:
                    wrap();
                    return false;
                default:
                    Assert.error();
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean disconnect() throws IOException
    {
        state = State.DISCONNECTING;

        if (!sslEngine.isOutboundDone())
        {
            sslEngine.closeOutbound();

            if (!flush())
                return false;
            
            outputNetBuffer.clear();

            SSLEngineResult result = sslEngine.wrap(emptyBuffer, outputNetBuffer);
            Assert.checkState(result.getStatus() == Status.CLOSED);

            outputNetBuffer.flip();
        }

        boolean res = flush() && sslEngine.isOutboundDone();
        if (res && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.disconnected());
        
        return res;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        if (state == State.DISCONNECTED || channel.socket().isInputShutdown())
            throw new ClosedChannelException();
        else if (state == State.NOT_CONNECTED || state == State.CONNECTING)
            throw new NotYetConnectedException();

        int n = readPacket();
        if (n <= 0)
            return n;

        if (!sslEngine.isInboundDone())
        {
            inputAppBuffer.clear();
            ByteBuffer[] buffers = new ByteBuffer[]{dst, inputAppBuffer};
            SSLEngineResult result = sslEngine.unwrap(inputNetBuffer, buffers, 0, 2);
            if (result.getStatus() == Status.CLOSED)
            {
                disconnect();
                return -1;
            }
            
            Assert.checkState(inputAppBuffer.position() == 0);
            Assert.checkState(result.getStatus() == Status.OK);
            Assert.checkState(!inputNetBuffer.hasRemaining());
            
            n = result.bytesProduced();
            if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.channelRead(n));
            return n;
        }

        if (state == State.DISCONNECTING)
            return -1;
        
        return 0;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
    {
        if (state == State.DISCONNECTED || channel.socket().isInputShutdown())
            throw new ClosedChannelException();
        else if (state == State.NOT_CONNECTED || state == State.CONNECTING)
            throw new NotYetConnectedException();
        
        int n = readPacket();
        if (n <= 0)
            return n;
        
        if (!sslEngine.isInboundDone())
        {
            inputAppBuffer.clear();
            ByteBuffer[] buffers = new ByteBuffer[length + 1];
            System.arraycopy(dsts, offset, buffers, 0, length);
            buffers[length] = inputAppBuffer;
            SSLEngineResult result = sslEngine.unwrap(inputNetBuffer, buffers, 0, buffers.length);
            if (result.getStatus() == Status.CLOSED)
            {
                disconnect();
                return -1;
            }
            
            Assert.checkState(inputAppBuffer.position() == 0);
            Assert.checkState(result.getStatus() == Status.OK);
            Assert.checkState(!inputNetBuffer.hasRemaining());

            n = result.bytesProduced();
            if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.channelRead(n));
            return n;
        }

        if (state == State.DISCONNECTING)
            return -1;
        
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException
    {
        if (state == State.DISCONNECTING || state == State.DISCONNECTED || channel.socket().isOutputShutdown())
            throw new ClosedChannelException();
        else if (state == State.NOT_CONNECTED || state == State.CONNECTING)
            throw new NotYetConnectedException();

        if (!flush())
            return 0;
        
        outputNetBuffer.clear();

        SSLEngineResult result = sslEngine.wrap(src, outputNetBuffer);

        outputNetBuffer.flip();
        
        Assert.checkState(result.getStatus() == Status.OK);

        int n = result.bytesConsumed();
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelWrite(n));
        
        flush();
        
        return n;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
    {
        if (state == State.DISCONNECTING || state == State.DISCONNECTED || channel.socket().isOutputShutdown())
            throw new ClosedChannelException();
        else if (state == State.NOT_CONNECTED || state == State.CONNECTING)
            throw new NotYetConnectedException();

        if (!flush())
            return 0;
        
        outputNetBuffer.clear();

        SSLEngineResult result = sslEngine.wrap(srcs, offset, length, outputNetBuffer);

        outputNetBuffer.flip();
        
        Assert.checkState(result.getStatus() == Status.OK);

        int n = result.bytesConsumed();
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelWrite(n));
        
        flush();
        
        return n;
    }

    @Override
    public boolean flush() throws IOException
    {
        if (outputNetBuffer.hasRemaining()) 
            super.write(outputNetBuffer);
        return !outputNetBuffer.hasRemaining();
    }
    
    @Override
    public boolean hasReadData()
    {
        return inputNetBuffer.hasRemaining() && inputAppBuffer.hasRemaining();
    }
    
    @Override
    public boolean hasWriteData()
    {
        return outputNetBuffer.hasRemaining();
    }

    protected TcpSslSocketChannel(SocketChannel channel, SSLEngine sslEngine, boolean clientMode, State state)
    {
        super(channel, state);
        
        Assert.notNull(sslEngine);
        
        sslEngine.setUseClientMode(clientMode);
        this.sslEngine = sslEngine;
        SSLSession sslSession = sslEngine.getSession();
        int appBufferSize = sslSession.getApplicationBufferSize();
        inputAppBuffer = ByteBuffer.allocate(appBufferSize);
        int netBufferSize = sslSession.getPacketBufferSize();
        inputNetBuffer = ByteBuffer.allocate(netBufferSize);
        outputNetBuffer = ByteBuffer.allocate(netBufferSize);
        emptyBuffer = ByteBuffer.allocate(0);

        inputAppBuffer.limit(0);
        inputNetBuffer.limit(0);
        outputNetBuffer.limit(0);
    }

    private void unwrap() throws IOException
    {
        int n = readPacket();
        if (n == 0)
            return;
        
        Assert.checkState(n > 0);
        inputAppBuffer.clear();
        SSLEngineResult result = sslEngine.unwrap(inputNetBuffer, inputAppBuffer);
        Assert.checkState(result.getStatus() == Status.OK);
        handshakeStatus = result.getHandshakeStatus();
    }

    private void wrap() throws IOException
    {
        if (!flush())
            return;
        
        outputNetBuffer.clear();

        SSLEngineResult result = sslEngine.wrap(emptyBuffer, outputNetBuffer);
        handshakeStatus = result.getHandshakeStatus();

        outputNetBuffer.flip();
        
        Assert.checkState(result.getStatus() == Status.OK);

        flush();
    }

    private int readPacket() throws IOException
    {
        if (!inputNetBuffer.hasRemaining())
        {
            inputNetBuffer.clear();
            inputNetBuffer.limit(5);
        }
        
        int n = super.read(inputNetBuffer);
        if (n < 0)
        {
            sslEngine.closeInbound();
            return -1;
        }
        
        if (inputNetBuffer.hasRemaining())
            return 0;
        
        inputNetBuffer.flip();
        
        if (inputNetBuffer.limit() == 5)
        {
            int packetLength = getPacketLength(inputNetBuffer);
            inputNetBuffer.limit(5 + packetLength);
            
            return readPacket();
        }
        else
            return 1;
    }

    private SSLEngineResult.HandshakeStatus runTasks()
    {
        while (true)
        {
            Runnable runnable = sslEngine.getDelegatedTask();
            if (runnable == null)
                break;
            
            runnable.run();
        }
        
        return sslEngine.getHandshakeStatus();
    }
    
    private static int getPacketLength(ByteBuffer buffer)
    {
        switch (buffer.get())
        {
        case 20:
        case 21:
        case 22:
        case 23:
            break;
        default:
            return -1;
        }

        if (buffer.get() == 3)
        {
            buffer.get();
            int packetLength = buffer.getShort();
            if (packetLength >= 0 && packetLength <= 0x8000)
                return packetLength;
        }
        
        return -1;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Handshake has been started.")
        ILocalizedMessage beginHandshake();
        @DefaultMessage("Channel has been connected with cipher suite ''{0}''.")
        ILocalizedMessage connected(String cipherSuite);
        @DefaultMessage("Channel has been disconnected.")
        ILocalizedMessage disconnected();
        @DefaultMessage("Data has been read: {0}.")
        ILocalizedMessage channelRead(long count);
        @DefaultMessage("Data has been written: {0}.")
        ILocalizedMessage channelWrite(long count);
    }
}
