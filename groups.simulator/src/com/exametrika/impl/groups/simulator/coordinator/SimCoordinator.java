/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.impl.ExpressionCondition;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
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
    private final CompileContext compileContext;
    private long simulationTime;
    private long timeIncrement = 100;
    private long lastUpdateTime;
    private boolean simulationStarted;
    private Map<String, Boolean> suspendConditions = new TreeMap<String, Boolean>();
    private Map<String, Boolean> logFilters = new TreeMap<String, Boolean>();
    private Map<String, ICondition<String>> waitConditions = new TreeMap<String, ICondition<String>>();
    private boolean waited;
    private Set<IAddress> respondingNodes = new LinkedHashSet<IAddress>(); 

    public SimCoordinator(SimCoordinatorChannel channel)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
        simulationTime = 0;
        this.compileContext = Expressions.createCompileContext(null);
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
            
            if ((actionResponse.getActionName().equals("stop") || actionResponse.getActionName().equals("log")) &&
                actionResponse.getResult() != null)
            {
                synchronized (this)
                {
                    for (ICondition<String> waitCondition : waitConditions.values())
                    {
                        if (waitCondition.evaluate((String)actionResponse.getResult()))
                        {
                            respondingNodes.remove(message.getSource());
                            if (respondingNodes.isEmpty())
                            {
                                waited = false;
                                notify();
                            }
                        }
                    }
                }
            }
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
        else if (actionName.equals("wait"))
        {
            synchronized (this)
            {
                String name = (String)parameters.get("name");
                if (parameters.containsKey("add"))
                {
                    String expression = (String)parameters.get("condition");
                    ICondition<String> condition = new ExpressionCondition<String>(Expressions.compile(expression, compileContext));
                    
                    waitConditions.put(name, condition);
                }
                else if (parameters.containsKey("remove"))
                    waitConditions.remove(name);
                else if (parameters.containsKey("list"))
                    shell.getWriter().write(waitConditions.keySet().toString() + "\n\n");
                else
                {
                    respondingNodes = channel.getAgents((List<String>)parameters.get("agentNamePattern"));
                    waited = true;
                    try
                    {
                        while (waited)
                            wait();
                    }
                    catch (InterruptedException e)
                    {
                        throw new ThreadInterruptedException(e);
                    }
                }
            }
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
        else if (actionName.equals("stop"))
        {
            if (parameters.containsKey("list"))
            {
                shell.getWriter().write(suspendConditions.keySet().toString() + "\n\n");
                return null;
            }
            simulationStarted = false;
            String name = (String)parameters.get("name");
            Boolean removed = (Boolean)parameters.get("remove");
            if (Boolean.TRUE.equals(removed))
                suspendConditions.remove(name);
            else
                suspendConditions.put(name, true);
        }
        else if (actionName.equals("log"))
        {
            if (parameters.containsKey("list"))
            {
                shell.getWriter().write(logFilters.keySet().toString() + "\n\n");
                return null;
            }
            String name = (String)parameters.get("name");
            Boolean removed = (Boolean)parameters.get("remove");
            if (Boolean.TRUE.equals(removed))
                logFilters.remove(name);
            else
                logFilters.put(name, true);
        }
        else if (actionName.equals("suspend"))
            simulationStarted = false;
        
        Set<IAddress> agentAddresses = channel.getAgents((List<String>)parameters.get("agentNamePattern"));
        channel.send(agentAddresses, new SimActionMessage(actionName, parameters));
        return null;
    }
}
