/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.simulator.agent.SimExecutor;

/**
 * The {@link SimulationProtocol} represents a simulation protocol.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimulationProtocol extends AbstractProtocol
{
    private final SimExecutor executor;

    public SimulationProtocol(String channelName, IMessageFactory messageFactory, SimExecutor executor)
    {
        super(channelName, null, messageFactory);
        
        Assert.notNull(executor);
        
        this.executor = executor;
    }

    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        executor.onInterceptSend(sender, message);
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        executor.onInterceptSend(feed, sink, message);
        return super.doSend(feed, sink, message);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        executor.onInterceptReceive(receiver, message);
        super.doReceive(receiver, message);
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
}
