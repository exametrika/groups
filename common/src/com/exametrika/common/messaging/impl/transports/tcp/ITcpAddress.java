/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetSocketAddress;

import com.exametrika.common.messaging.IAddress;


/**
 * The {@link ITcpAddress} is a TCP address.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpAddress extends IAddress
{
    /**
     * Returns TCP socket address.
     *
     * @return TCP socket address
     */
    InetSocketAddress getAddress();
}
