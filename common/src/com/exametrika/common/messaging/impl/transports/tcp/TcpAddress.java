/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetSocketAddress;
import java.util.UUID;

import com.exametrika.common.messaging.impl.transports.AbstractAddress;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpAddress} represents a TCP address implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpAddress extends AbstractAddress implements ITcpAddress
{
    private final InetSocketAddress address;
    private final String connection;

    public TcpAddress(UUID id, InetSocketAddress address, String name)
    {
        super(id, name);
        
        Assert.notNull(address);
        Assert.notNull(address.getAddress());

        this.address = address;
        this.connection = TcpTransport.getCanonicalConnectionAddress(address);
    }
    
    @Override
    public InetSocketAddress getAddress()
    {
        return address;
    }

    @Override
    public String getConnection()
    {
        return connection;
    }
}
