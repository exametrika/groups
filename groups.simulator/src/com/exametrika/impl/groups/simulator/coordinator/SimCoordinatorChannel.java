/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.groups.simulator.messages.SimActionMessageSerializer;
import com.exametrika.impl.groups.simulator.messages.SimActionResponseMessageSerializer;



/**
 * The {@link SimCoordinatorChannel} represents a simulator coordinator channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinatorChannel implements IReceiver, IChannelListener, ISerializationRegistrar,  
    ILifecycle, ITimeService, ICompartmentTimerProcessor
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(SimCoordinatorChannel.class);
    private final IMarker marker;
    private final SimCoordinator coordinator;
    private boolean started;
    private IChannel channel;
    private volatile long currentTime;
    private volatile Map<IAddress, SimCoordinatorAgentChannel> agents = new LinkedHashMap<IAddress, SimCoordinatorAgentChannel>();

    public SimCoordinatorChannel(String chanelName)
    {
        marker = Loggers.getMarker(chanelName);
        coordinator = new SimCoordinator(this);
    }
    
    public void setChannel(IChannel channel)
    {
        Assert.notNull(channel);
        Assert.isNull(this.channel);
        
        this.channel = channel;
    }
    
    public SimCoordinator getCoordinator()
    {
        return coordinator;
    }
    
    public Map<IAddress, SimCoordinatorAgentChannel> getAgents()
    {
        return agents;
    }
    
    public SimCoordinatorAgentChannel findAgent(String name)
    {
        Map<IAddress, SimCoordinatorAgentChannel> agents = this.agents;
        for (Map.Entry<IAddress, SimCoordinatorAgentChannel> entry : agents.entrySet())
        {
            if (entry.getKey().getName().equals(name))
                return entry.getValue();
        }
        
        return null;
    }

    @Override
    public void onTimer(long currentTime)
    {
        this.currentTime = currentTime;
        coordinator.onTimer(currentTime);
    }

    @Override
    public void start()
    {
        synchronized (this)
        {
            Assert.checkState(channel != null);
            Assert.checkState(!started);
            
            started = true;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.started());
        }
        
        channel.start();
    }

    @Override
    public void stop()
    {
        Map<IAddress, SimCoordinatorAgentChannel> agents;
        synchronized (this)
        {
            if (!started)
                return;
            
            started = false;
            agents = this.agents;
            this.agents = new LinkedHashMap<IAddress, SimCoordinatorAgentChannel>();
        }
        
        channel.stop();
        
        for (SimCoordinatorAgentChannel agent : agents.values())
            agent.close();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public synchronized void onNodeConnected(IAddress node)
    {
        Assert.isTrue(!agents.containsKey(node));
        Map<IAddress, SimCoordinatorAgentChannel> agents = new LinkedHashMap<IAddress, SimCoordinatorAgentChannel>(this.agents);
        SimCoordinatorAgentChannel agent = new SimCoordinatorAgentChannel(channel, node, marker);
        agents.put(node, agent);
        this.agents = agents;
    }

    @Override
    public void onNodeFailed(IAddress address)
    {
        SimCoordinatorAgentChannel agent;
        synchronized (this)
        {
            Map<IAddress, SimCoordinatorAgentChannel> agents = new LinkedHashMap<IAddress, SimCoordinatorAgentChannel>(this.agents);
            agent = agents.remove(address);
            this.agents = agents;
        }
        if (agent != null)
            agent.close();
    }

    @Override
    public void onNodeDisconnected(IAddress address)
    {
        onNodeFailed(address);
    }

    @Override
    public void receive(IMessage message)
    {
        coordinator.receive(message);
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new SimActionMessageSerializer());
        registry.register(new SimActionResponseMessageSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(SimActionMessageSerializer.ID);
        registry.unregister(SimActionResponseMessageSerializer.ID);
    }

    @Override
    public long getCurrentTime()
    {
        return currentTime;
    }
    
    public void send(List<String> agentNamePattern, IMessagePart part)
    {
        List<ICondition<String>> filters = null;
        if (agentNamePattern != null)
        {
            filters = new ArrayList<ICondition<String>>();
            for (String filter : agentNamePattern)
                filters.add(Strings.createFilterCondition(filter, true));
        }
        
        Map<IAddress, SimCoordinatorAgentChannel> agents = this.agents;
        for (Map.Entry<IAddress, SimCoordinatorAgentChannel> entry : agents.entrySet())
        {
            boolean allowed;
            if (filters != null)
            {
                allowed = false;
                for (ICondition<String> filter : filters)
                {
                    if (filter.evaluate(entry.getKey().getName()))
                    {
                        allowed = true;
                        break;
                    }
                }
            }
            else
                allowed = true;
            
            if (allowed)
                entry.getValue().send(part);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Simulator coordinator channel is started.")
        ILocalizedMessage started();
        
        @DefaultMessage("Simulator coordinator channel is stopped.")
        ILocalizedMessage stopped();
    }
}
