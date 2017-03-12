/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import java.util.ArrayList;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link UnicastAddress} represents a TCP address implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UnicastAddress extends AbstractAddress
{
    private final ArrayList<Object> addresses = new ArrayList<Object>();
    private final ArrayList<String> connections = new ArrayList<String>();

    public UnicastAddress(UUID id, String name)
    {
        super(id, name);
    }
    
    @Override
    public int getCount()
    {
        return addresses.size();
    }
    
    public <T> T getAddress(int transportId)
    {
        return (T)Collections.get(addresses, transportId);
    }
    
    public void setAddress(int transportId, Object address, String connection)
    {
        Assert.notNull(address);
        Assert.notNull(connection);
        
        Collections.set(addresses, transportId, address);
        Collections.set(connections, transportId, connection);
    }

    @Override
    public String getConnection(int transportId)
    {
        return Collections.get(connections, transportId);
    }
}
