/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannelHandshaker;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link TcpChannelHandshaker} represents a tcp channel handshaker.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpChannelHandshaker implements ITcpChannelHandshaker<TcpPacket>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(TcpChannelHandshaker.class);
    private final TcpConnection connection;
    private State state;
    private TcpPacket packet;

    public TcpChannelHandshaker(TcpConnection connection)
    {
        Assert.notNull(connection);

        this.connection = connection;

        if (connection.isClient())
            state = State.CLIENT_SEND;
        else
            state = State.SERVER_RECEIVE;
    }

    public void logState()
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, connection.getChannel().getMarker(), messages.state(state));
    }

    public void setDuplicate()
    {
        state = State.DUPLICATE_SEND;
        logState();
    }

    @Override
    public boolean handshake(ITcpPacketChannel<TcpPacket> channel)
    {
        switch (state)
        {
        case CLIENT_SEND:
            if (packet == null)
                packet = serialize();

            if (!channel.write(packet))
                return false;

            state = State.CLIENT_RECEIVE;
            packet = null;
            logState();
            return false;
        case CLIENT_RECEIVE:
            packet = channel.read();
            if (packet == null)
                return false;

            deserialize(packet);
            state = State.CONNECTED;
            packet = null;
            logState();
            return true;
        case SERVER_RECEIVE:
            packet = channel.read();
            if (packet == null)
                return false;

            deserialize(packet);

            if (!connection.getTransport().addServerConnection(connection))
                return handshake(channel);

            state = State.SERVER_SEND;
            packet = null;
            logState();
            return handshake(channel);
        case SERVER_SEND:
            if (packet == null)
                packet = serialize();

            if (!channel.write(packet))
                return false;

            state = State.CONNECTED;
            packet = null;
            logState();
            return true;
        case DUPLICATE_SEND:
            {
                ByteArray packet = new ByteArray(new byte[1]);
                packet.getBuffer()[0] = TcpTransport.FLAG_DUPLICATE;

                if (!channel.write(new TcpPacket(packet)))
                    return false;
                
                state = State.DISCONNECTING_RECEIVE;
                logState();
                channel.disconnect();
                return false;
            }
        default:
            Assert.error();
            return false;
        }
    }

    @Override
    public boolean canDisconnect(TcpPacket packet)
    {
        Assert.isTrue(packet.getBuffers().size() == 1);
        byte flags = packet.getBuffers().get(0).getBuffer()[0];
        if ((flags & TcpTransport.FLAG_DUPLICATE) == TcpTransport.FLAG_DUPLICATE)
            connection.setDuplicate();
        
        if ((flags & TcpTransport.FLAG_DISCONNECT_REQUEST) == TcpTransport.FLAG_DISCONNECT_REQUEST || 
            (flags & TcpTransport.FLAG_DUPLICATE) == TcpTransport.FLAG_DUPLICATE)
        {
            if (state != State.DISCONNECTED && state != State.DISCONNECTING_RECEIVE)
            {
                state = State.DISCONNECTING_SEND;
                logState();
            }
            return true;
        }
        
        if ((flags & TcpTransport.FLAG_DISCONNECT_RESPONSE) == TcpTransport.FLAG_DISCONNECT_RESPONSE)
            return true;

        return false;
    }

    @Override
    public boolean disconnect(ITcpPacketChannel<TcpPacket> channel)
    {
        switch (state)
        {
        case DISCONNECTED:
            return true;
        case DISCONNECTING_RECEIVE:
            {
                TcpPacket packet = channel.read();
                if (packet == null)
                    return false;
                
                Assert.isTrue(packet.getBuffers().size() == 1);
                byte flags = packet.getBuffers().get(0).getBuffer()[0];
                
                if ((flags & TcpTransport.FLAG_DISCONNECT_RESPONSE) == TcpTransport.FLAG_DISCONNECT_RESPONSE)
                {
                    state = State.DISCONNECTED;
                    logState();
                    return true;
                }
                
                if ((flags & TcpTransport.FLAG_DISCONNECT_REQUEST) == TcpTransport.FLAG_DISCONNECT_REQUEST || 
                    (flags & TcpTransport.FLAG_DUPLICATE) == TcpTransport.FLAG_DUPLICATE)
                {
                    if (TcpAddress.compare(connection.getLocalAddress().getAddress(), connection.getRemoteInetAddress()) < 0)
                    {
                        state = State.DISCONNECTING_SEND;
                        logState();
                    }
                }
                return false;
            }
        case DISCONNECTING_SEND:
            {
                channel.read();
                ByteArray packet = new ByteArray(new byte[1]);
                packet.getBuffer()[0] = TcpTransport.FLAG_DISCONNECT_RESPONSE;
    
                if (!channel.write(new TcpPacket(packet)))
                    return false;
                
                state = State.DISCONNECTED;
                logState();
                return true;
            }
        default:
            ByteArray packet = new ByteArray(new byte[1]);
            packet.getBuffer()[0] = TcpTransport.FLAG_DISCONNECT_REQUEST;

            if (!channel.write(new TcpPacket(packet)))
                return false;
            
            state = State.DISCONNECTING_RECEIVE;
            logState();
            return false;
        }
    }

    private TcpPacket serialize()
    {
        ByteOutputStream outputStream = new ByteOutputStream();
        outputStream.grow(1);
        DataSerialization serialization = new DataSerialization(outputStream);

        UUID id = connection.getLocalAddress().getId();
        Serializers.writeUUID(serialization, id);
        serialization.writeString(connection.getLocalAddress().getName());

        if (connection.isClient())
        {
            InetSocketAddress localAddress = connection.getLocalAddress().getAddress();
            serialization.writeInt(localAddress.getPort());
            serialization.writeByteArray(new ByteArray(localAddress.getAddress().getAddress()));
        }

        return new TcpPacket(new ByteArray(outputStream.getBuffer(), 0, outputStream.getLength()));
    }

    private void deserialize(TcpPacket packet)
    {
        Assert.isTrue(packet.getBuffers().size() == 1);
        ByteArray buffer = packet.getBuffers().get(0);
        ByteInputStream inputStream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset() + 1, buffer.getLength() - 1);
        DataDeserialization deserialization = new DataDeserialization(inputStream);

        UUID id = Serializers.readUUID(deserialization);
        String name = deserialization.readString();

        if (!connection.isClient())
        {
            int port = deserialization.readInt();
            ByteArray addressData = deserialization.readByteArray();

            try
            {
                TcpAddress remoteAddress = new TcpAddress(id, new InetSocketAddress(
                    InetAddress.getByAddress(addressData.toByteArray()), port), name);
                connection.setRemoteAddress(remoteAddress);
            }
            catch (UnknownHostException e)
            {
                throw new SerializationException(e);
            }
        }
        else
            connection.setRemoteAddress(new TcpAddress(id, connection.getRemoteInetAddress(), name));
    }

    private enum State
    {
        CLIENT_SEND, 
        CLIENT_RECEIVE, 
        SERVER_RECEIVE, 
        SERVER_SEND, 
        CONNECTED, 
        DUPLICATE_SEND,
        DISCONNECTING_SEND,
        DISCONNECTING_RECEIVE,
        DISCONNECTED
    }
    
    private interface IMessages
    {
        @DefaultMessage("Current state ''{0}''.")
        ILocalizedMessage state(State state);
    }
}
