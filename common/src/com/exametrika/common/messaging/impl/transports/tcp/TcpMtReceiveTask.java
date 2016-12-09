/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpMtReceiveTask} represents a receive task.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class TcpMtReceiveTask
{
    protected final TcpConnection connection;
    protected volatile TcpPacket packet;
    
    public TcpMtReceiveTask(TcpConnection connection, TcpPacket packet)
    {
        Assert.notNull(connection);
        
        this.connection = connection;
        this.packet = packet;
    }
    
    public TcpConnection getConnection()
    {
        return connection;
    }
    
    public TcpPacket getPacket()
    {
        return packet;
    }
    
    public void onCompleted()
    {
    }
}