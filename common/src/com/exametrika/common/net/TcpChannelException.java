/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpChannelException} is thrown when TCP channel exception occured.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class TcpChannelException extends TcpException
{
    private final ITcpChannel channel;
    
    public TcpChannelException(ITcpChannel channel)
    {
        Assert.notNull(channel);
        this.channel = channel;
    }

    public TcpChannelException(ITcpChannel channel, ILocalizedMessage message)
    {
        super(message);
        
        Assert.notNull(channel);
        this.channel = channel;
    }

    public TcpChannelException(ITcpChannel channel, ILocalizedMessage message, Throwable cause)
    {
        super(message, cause);
        
        Assert.notNull(channel);
        this.channel = channel;
    }

    public TcpChannelException(ITcpChannel channel, Throwable cause)
    {
        super(cause);
        
        Assert.notNull(channel);
        this.channel = channel;
    }
    
    public ITcpChannel getChannel()
    {
        return channel;
    }
}
