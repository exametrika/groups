/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;

/**
 * The {@link SimExecutor} represents a simulator executor.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimExecutor
{
    public void onTimer(long currentTime)
    {
        // TODO:
    }
    
    public void onInterceptSend(ISender sender, IMessage message)
    {
    }
    
    public void onInterceptSend(IFeed feed, ISink sink, IMessage message)
    {
    }

    public void onInterceptReceive(IReceiver receiver, IMessage message)
    {
        
    }
    
    public void onActionReceive(IMessage message)
    {
        // TODO:
    }
    
    public void onNodeFailed(IAddress address)
    {
        // TODO:
    }
}
