/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import com.exametrika.common.log.IMarker;



/**
 * The {@link ITcpSocketChannel} is a TCP socket channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpSocketChannel extends Closeable
{
    void setMarker(IMarker marker);
    
    int validOps();
    
    Socket socket();
    
    boolean isConnected();
    
    boolean connect(SocketAddress remote) throws IOException;
    
    boolean finishConnect() throws IOException;
    
    int read(ByteBuffer dst) throws IOException;
    
    long read(ByteBuffer[] dsts, int offset, int length) throws IOException;
    
    long read(ByteBuffer[] dsts) throws IOException;
    
    int write(ByteBuffer src) throws IOException;
    
    long write(ByteBuffer[] srcs, int offset, int length) throws IOException;
    
    long write(ByteBuffer[] srcs) throws IOException;
    
    boolean isRegistered();
    
    SelectionKey keyFor(ITcpSelector sel);
    
    SelectionKey register(ITcpSelector sel, int ops, Object att) throws ClosedChannelException;
    
    SelectionKey register(ITcpSelector sel, int ops) throws ClosedChannelException;
    
    boolean isBlocking();
    
    Object blockingLock();
    
    void configureBlocking(boolean block) throws IOException;
    
    boolean isOpen();
    
    boolean handshake() throws IOException;
    
    boolean disconnect() throws IOException;
    
    boolean flush() throws IOException;
    
    boolean hasReadData();
    
    boolean hasWriteData();
}
