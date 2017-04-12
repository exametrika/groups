/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.util.List;
import java.util.Map;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.simulator.messages.SimActionMessage;
import com.exametrika.impl.groups.simulator.messages.SimActionResponseMessage;




/**
 * The {@link SimCoordinator} represents a simulator coordinator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinator
{
    private IShell shell;
    private final SimCoordinatorChannel channel;
    private final long updateTimePeriod = 10000;
    private long simulationTime;
    private long timeIncrement = 100;
    private long lastUpdateTime;
    private boolean simulationStarted;

    public SimCoordinator(SimCoordinatorChannel channel)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
        simulationTime = 0;
    }

    public void setShell(IShell shell)
    {
        Assert.notNull(shell);
        Assert.isNull(this.shell);
        
        this.shell = shell;
    }
    
    public SimCoordinatorChannel getChannel()
    {
        return channel;
    }
    
    public void onTimer(long currentTime)
    {
        if (simulationStarted && currentTime > lastUpdateTime + updateTimePeriod)
        {
            lastUpdateTime = currentTime;
            simulationTime += timeIncrement;
            
            channel.send(null, new SimActionMessage("time", new MapBuilder().put("delta", timeIncrement).toMap()));
        }
    }
    
    public void receive(IMessage message)
    {
        if (message.getPart() instanceof SimActionResponseMessage)
        {
            SimActionResponseMessage actionResponse = message.getPart();
            if ((actionResponse.getActionName().equals("print") || actionResponse.getActionName().equals("log")) &&
                actionResponse.getResult() != null)
                shell.getWriter().write(actionResponse.getResult().toString() + "\n\n");
        }
    }
    
    public Object execute(String actionName, Map<String, Object> parameters)
    {
        if (actionName.equals("time"))
        {
            long period = simulationTime;
            long hours = period / (60 * 60 * 1000);
            long minutes = (period - hours * (60 * 60 * 1000)) / (60 * 1000);
            long seconds = (period - hours * (60 * 60 * 1000) - minutes * (60 * 1000)) / 1000;
            long millis = period - hours * (60 * 60 * 1000) - minutes * (60 * 1000) - seconds * 1000;
            return hours + ":" + minutes + ":" + seconds + "." + millis;
        }
        else if (actionName.equals("sleep"))
        {
            Threads.sleep((Long)parameters.get("period"));
            return null;
        }
        else if (actionName.equals("timeSpeed"))
        {
            timeIncrement = (long)parameters.get("timeIncrement");
            return null;
            
        }
        
        if (actionName.equals("start"))
            timeIncrement = (long)parameters.get("timeIncrement");
        
        if (actionName.equals("start") || actionName.equals("resume"))
            simulationStarted = true;
        else if (actionName.equals("stop") || actionName.equals("suspend"))
            simulationStarted = false;
        
        channel.send((List<String>)parameters.get("agentNamePattern"), new SimActionMessage(actionName, parameters));
        return null;
    }
}
