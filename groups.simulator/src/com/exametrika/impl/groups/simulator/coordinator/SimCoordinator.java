/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;




/**
 * The {@link SimCoordinator} represents a simulator coordinator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinator
{
    private final SimCoordinatorChannel channel;

    public SimCoordinator(SimCoordinatorChannel channel)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
    }
    
    public void onTimer(long currentTime)
    {
    }
    
    public void receive(final IMessage message)
    {
    }
    
    public void start(long delay, double timeSpeed)
    {
        
    }
    
    public void stop(String condition)
    {
        
    }
    
    public void suspend()
    {
        
    }
    
    public void resume()
    {
        
    }
    
    
}
