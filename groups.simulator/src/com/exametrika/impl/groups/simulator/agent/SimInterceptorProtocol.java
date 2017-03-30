/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link SimInterceptorProtocol} represents a protocol that performs interception of messages to simulation runtime.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimInterceptorProtocol extends AbstractProtocol
{
    private SimExecutor executor;
    
    public SimInterceptorProtocol(String channelName, IMessageFactory messageFactory)
    {
        super(channelName, messageFactory);
    }
    
    public void setExecutor(SimExecutor executor)
    {
        Assert.notNull(executor);
        Assert.isNull(this.executor);
        
        this.executor = executor;
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        executor.intercept(message);
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        executor.intercept(message);
        return super.doSend(feed, sink, message);
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
}
