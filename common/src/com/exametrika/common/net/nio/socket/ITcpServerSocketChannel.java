/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;



/**
 * The {@link ITcpServerSocketChannel} is a TCP server socket channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpServerSocketChannel extends Closeable
{
    int validOps();
    
    ServerSocket socket();
    
    ITcpSocketChannel accept() throws IOException;
    
    boolean isRegistered();
    
    SelectionKey keyFor(ITcpSelector sel);
    
    SelectionKey register(ITcpSelector sel, int ops, Object att) throws ClosedChannelException;
    
    SelectionKey register(ITcpSelector sel, int ops) throws ClosedChannelException;
    
    boolean isBlocking();
    
    Object blockingLock();
    
    void configureBlocking(boolean block) throws IOException;
    
    boolean isOpen();
}
