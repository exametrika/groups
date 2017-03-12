/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpUtils} represents a tcp utils.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpUtils
{
    public static String getCanonicalConnectionAddress(InetSocketAddress address)
    {
        return "tcp://" + address.getAddress().getCanonicalHostName() + ":" + address.getPort();
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
    
    private TcpUtils()
    {
    }
}
