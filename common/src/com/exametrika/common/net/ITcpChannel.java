/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.common.log.IMarker;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;



/**
 * The {@link ITcpChannel} is a TCP channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpChannel
{
    /**
     * Initialization parameters.
     */
    public class Parameters
    {
        /** Channel listeners. */
        public Set<ITcpChannelListener> channelListeners = new LinkedHashSet<ITcpChannelListener>();
        /** Channel reader. */
        public ITcpChannelReader channelReader;
        /** Channel writer. */
        public ITcpChannelWriter channelWriter;
        /** Channel rate controller. Can be <c>null<c> if not set. */
        public ITcpRateControllerFactory rateController;
        /** Channel name. Can be <c>null<c> if not set. */
        public String name;
        /** User-specified data to be attached to channel. Can be <c>null<c> if not set. */
        public Object data;
        /** Receive buffer size. Can be <c>null<c> if not set. */
        public Integer receiveBufferSize; 
        /** Send buffer size. Can be <c>null<c> if not set. */
        public Integer sendBufferSize;
        /** Traffic class. Can be <c>null<c> if not set. */
        public Integer trafficClass;
        /** Socket channel factory. Ignored when connection is being accepted.*/
        public ITcpSocketChannelFactory socketChannelFactory = new TcpSocketChannelFactory();
    }

    /**
     * Returns dispatcher.
     *
     * @return dispatcher
     */
    ITcpDispatcher getDispatcher();
    
    /**
     * Returns channel name.
     *
     * @return channel name or null is channel does not have a name
     */
    String getName();
    
    /**
     * Sets channel name.
     *
     * @param name channel name
     */
    void setName(String name);
    
    /**
     * Returns channel marker.
     *
     * @return channel marker
     */
    IMarker getMarker();
    
    /**
     * Returns user-specified data, attached to this channel.
     *
     * @param <T> data type
     * @return user-specified data, attached to this channel or null if there are no such data
     */
    <T> T getData();
    
    /**
     * Sets user-specified data for this channel.
     *
     * @param <T> data type
     * @param data user-specified data, attached to this channel. Can be null
     */
    <T> void setData(T data);

    /**
     * Is channel connected?
     *
     * @return true if channel is connected, false - if channel is not connected
     */
    boolean isConnected();
    
    /**
     * Is channel disconnected?
     *
     * @return true if channel is disconnected, false - if channel is not disconnected yet
     */
    boolean isDisconnected();
    
    /**
     * Get remote address channel connected to.
     *
     * @return channel remote address
     */
    InetSocketAddress getRemoteAddress();
    
    /**
     * Get local address channel bound to.
     *
     * @return channel local address
     */
    InetSocketAddress getLocalAddress();
    
    /**
     * Returns channel's last read time.
     *
     * @return channel's last read time
     */
    long getLastReadTime();
    
    /**
     * Returns channel's last write time.
     *
     * @return channel's last write time
     */
    long getLastWriteTime();
    
    /**
     * Is channel used for administrative purposes? Some channel restrictions (i.e. rate control...) don't apply to admin channels.
     *
     * @return true if channel is admin channel
     */
    boolean isAdmin();
    
    /**
     * Sets channel as admin channel.
     */
    void setAdmin();
    
    /**
     * Updates status of read operations for this channel.
     */
    void updateReadStatus();
    
    /**
     * Updates status of write operations for this channel.
     */
    void updateWriteStatus();
    
    /**
     * Gracefully disconnects and closes channel.
     */
    void disconnect();
    
    /**
     * Forcefully closes channel.
     */
    void close();
}
