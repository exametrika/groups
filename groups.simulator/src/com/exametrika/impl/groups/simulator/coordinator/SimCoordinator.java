/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.impl.groups.simulator.messages.ActionMessage;




/**
 * The {@link SimCoordinator} represents a simulator coordinator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinator
{
    private final SimCoordinatorChannel channel;
    private final long updateTimePeriod = 10000;
    private long startTime;
    private long simulationTime;
    private long timeIncrement = 100;
    private long lastUpdateTime;

    public SimCoordinator(SimCoordinatorChannel channel)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
        startTime = channel.getCurrentTime();
        simulationTime = startTime;
    }
    
    public void onTimer(long currentTime)
    {
        if (currentTime > lastUpdateTime + updateTimePeriod)
        {
            lastUpdateTime = currentTime;
            simulationTime =+ timeIncrement;
            
            channel.send(null, new ActionMessage("time", new MapBuilder().put("value", simulationTime).toMap()));
        }
    }
    
    public void receive(final IMessage message)
    {
    }
    
    public void start(String agentNamePattern, long delay, long timeIncrement)
    {
        this.timeIncrement = timeIncrement;
        channel.send(agentNamePattern, new ActionMessage("start", new MapBuilder().put("delay", delay).toMap()));
    }
    
    public void delay(String agentNamePattern, long period, boolean oneTime)
    {
        channel.send(agentNamePattern, new ActionMessage("delay", new MapBuilder().put("period", period).put("oneTime", oneTime).toMap()));
    }
    
    public void timeSpeed(long increment)
    {
        this.timeIncrement = increment;
    }
    
    public String time()
    {
        long period = simulationTime - startTime;
        long hours = period / (60 * 60 * 1000);
        long minutes = (period - hours * (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (period - hours * (60 * 60 * 1000) - minutes * (60 * 1000)) / 1000;
        long millis = period - hours * (60 * 60 * 1000) - minutes * (60 * 1000) - seconds * 1000;
        return hours + ":" + minutes + ":" + seconds + "." + millis;
    }
    
    public void stop(String agentNamePattern, String condition)
    {
        channel.send(agentNamePattern, new ActionMessage("stop", new MapBuilder().put("condition", condition).toMap()));
    }
    
    public void suspend(String agentNamePattern)
    {
        channel.send(agentNamePattern, new ActionMessage("suspend", new MapBuilder().toMap()));
    }
    
    public void resume(String agentNamePattern)
    {
        channel.send(agentNamePattern, new ActionMessage("resume", new MapBuilder().toMap()));
    }
    
    public void print(String agentNamePattern, String expression)
    {
        channel.send(agentNamePattern, new ActionMessage("print", new MapBuilder().put("expression", expression).toMap()));
    }
    
    public void log(String agentNamePattern, String filter, String expression, boolean enabled)
    {
        channel.send(agentNamePattern, new ActionMessage("log", new MapBuilder().put("filter", filter).put("expression", 
            expression).put("enabled", enabled).toMap()));
    }
    
    public void kill(String agentNamePattern)
    {
        channel.send(agentNamePattern, new ActionMessage("kill", new MapBuilder().toMap()));
    }
}
