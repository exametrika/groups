/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
    
    public static int compare(InetSocketAddress address1, InetSocketAddress address2)
    {
        ByteBuffer inetAddress1 = ByteBuffer.wrap(address1.getAddress().getAddress());
        ByteBuffer inetAddress2 = ByteBuffer.wrap(address2.getAddress().getAddress());
        
        int res = inetAddress1.compareTo(inetAddress2);
        if (res != 0)
            return res;
        
        int port1 = address1.getPort();
        int port2 = address2.getPort();
        
        if (port1 > port2)
            return 1;
        else if (port1 == port2)
        {
            // Addresses can not be equal
            Assert.error();
            return 0;
        }
        else
            return -1;
    }
}
