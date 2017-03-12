/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link ConnectionManager} is an implementation of {@link IConnectionProvider}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ConnectionManager implements IConnectionProvider
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int transportId;
    private final Map<String, IConnectionProvider> providers;

    public ConnectionManager(int transportId, Map<String, IConnectionProvider> providers)
    {
        Assert.notNull(providers);
        
        this.transportId = transportId;
        this.providers = providers;
    }
    
    @Override
    public void connect(String connection)
    {
        Assert.notNull(connection);
        
        String schema;
        String addressPart;
        int pos = connection.indexOf("://");
        if (pos != -1)
        {
            schema = connection.substring(0, pos);
            addressPart = connection.substring(pos + 3);
        }
        else
        {
            schema = "tcp";
            addressPart = connection;
        }
            
        IConnectionProvider provider = providers.get(schema);
        if (provider != null)
            provider.connect(addressPart);
        else
            throw new InvalidArgumentException(messages.schemaNotSupported(schema));
    }
    
    @Override
    public void connect(IAddress address)
    {
        Assert.notNull(address);
        
        String connection = address.getConnection(transportId);
        
        String schema;
        int pos = connection.indexOf("://");
        if (pos != -1)
            schema = connection.substring(0, pos);
        else
            schema = "tcp";
            
        IConnectionProvider provider = providers.get(schema);
        if (provider != null)
            provider.connect(address);
        else
            throw new InvalidArgumentException(messages.schemaNotSupported(schema));
    }
    
    @Override
    public void disconnect(String connection)
    {
        Assert.notNull(connection);
        
        String schema;
        String addressPart;
        int pos = connection.indexOf("://");
        if (pos != -1)
        {
            schema = connection.substring(0, pos);
            addressPart = connection.substring(pos + 3);
        }
        else
        {
            schema = "tcp";
            addressPart = connection;
        }
            
        IConnectionProvider provider = providers.get(schema);
        if (provider != null)
            provider.disconnect(addressPart);
        else
            throw new InvalidArgumentException(messages.schemaNotSupported(schema));
    }
    
    @Override
    public void disconnect(IAddress address)
    {
        Assert.notNull(address);
        
        String connection = address.getConnection(transportId);
        
        String schema;
        int pos = connection.indexOf("://");
        if (pos != -1)
            schema = connection.substring(0, pos);
        else
            schema = "tcp";
            
        IConnectionProvider provider = providers.get(schema);
        if (provider != null)
            provider.disconnect(address);
        else
            throw new InvalidArgumentException(messages.schemaNotSupported(schema));
    }
    
    @Override
    public String canonicalize(String connection)
    {
        Assert.notNull(connection);
        
        String schema;
        String addressPart;
        int pos = connection.indexOf("://");
        if (pos != -1)
        {
            schema = connection.substring(0, pos);
            addressPart = connection.substring(pos + 3);
        }
        else
        {
            schema = "tcp";
            addressPart = connection;
        }
            
        IConnectionProvider provider = providers.get(schema);
        if (provider != null)
            return provider.canonicalize(addressPart);
        else
            throw new InvalidArgumentException(messages.schemaNotSupported(schema));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Address schema ''{0}'' is not supported.")
        ILocalizedMessage schemaNotSupported(String schema);
    }
}
