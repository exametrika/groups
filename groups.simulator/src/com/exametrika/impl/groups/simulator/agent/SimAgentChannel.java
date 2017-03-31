/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import java.io.File;
import java.util.List;

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
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.impl.groups.simulator.messages.ActionMessage;
import com.exametrika.impl.groups.simulator.messages.ActionMessageSerializer;
import com.exametrika.impl.groups.simulator.messages.ActionResponseMessageSerializer;



/**
 * The {@link SimAgentChannel} represents a simulator agent channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimAgentChannel implements IReceiver, IChannelListener, ISerializationRegistrar, ILifecycle, ICompartmentTimerProcessor
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(SimAgentChannel.class);
    private final String host;
    private final int port;
    private final long minReconnectPeriod;
    private final IMarker marker;
    private final SimExecutor executor;
    private IChannel channel;
    private volatile State state;
    private long currentTime;
    private IAddress address;
    private long lastConnectTime;
    
    public SimAgentChannel(String channelName, String host, int port)
    {
        this.host = host;
        this.port = port;
        minReconnectPeriod = 60000;
        executor = new SimExecutor(this);
        marker = Loggers.getMarker(channelName);
    }
    
    public IAddress getAddress()
    {
        return address;
    }
    
    public void setChannel(IChannel channel)
    {
        Assert.notNull(channel);
        Assert.isNull(this.channel);
        
        this.channel = channel;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        this.currentTime = currentTime;
        
        if (state == State.STARTED && (lastConnectTime == 0 || (currentTime - lastConnectTime) > minReconnectPeriod))
            connect();
        
        executor.onTimer(currentTime);
    }

    @Override
    public void start()
    {
        Assert.checkState(channel != null);
        Assert.isNull(state);
        
        this.state = State.STARTED;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());
        
        channel.start();
        
        connect();
    }

    @Override
    public void stop()
    {
        if (state == null)
            return;
        
        disconnect();
        state = null;
        
        channel.stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public void onNodeConnected(IAddress node)
    {
        state = State.CONNECTED;
        address = node;
            
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.connected(node));
    }

    @Override
    public void onNodeFailed(IAddress node)
    {
        disconnect();
    }

    @Override
    public void onNodeDisconnected(IAddress node)
    {
        disconnect();
    }

    @Override
    public void receive(final IMessage message)
    {
        if (message.getPart() instanceof ActionMessage)
            executor.onActionReceived((ActionMessage)message.getPart());
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new ActionMessageSerializer());
        registry.register(new ActionResponseMessageSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(ActionMessageSerializer.ID);
        registry.unregister(ActionResponseMessageSerializer.ID);
     }

    public void send(IMessagePart part)
    {
        IMessage message = channel.getMessageFactory().create(address, part);
        channel.send(message);    
    }
    
    public void send(IMessagePart part, List<File> files)
    {
        IMessage message = channel.getMessageFactory().create(address, part, 0, files);
        channel.send(message);    
    }
    
    private void connect()
    {
        String agentAddress = "tcp://" + host + ":" + port;
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.connecting(agentAddress));
        
        channel.connect(agentAddress);
        lastConnectTime = currentTime;
    }

    private void disconnect()
    {
        state = State.STARTED;
        lastConnectTime = currentTime;
        if (address != null)
            executor.onDisconnected();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.disconnected());
    }

    private enum State
    {
        STARTED,
        CONNECTED
    }
    
    private interface IMessages
    {
        @DefaultMessage("Simulator agent channel is started.")
        ILocalizedMessage started();
        
        @DefaultMessage("Simulator agent channel is connecting to coordinator ''{0}''.")
        ILocalizedMessage connecting(String agent);
        
        @DefaultMessage("Simulator agent channel is connected to coordinator ''{0}''.")
        ILocalizedMessage connected(IAddress agent);
        
        @DefaultMessage("Simulator agent channel is disconnected.")
        ILocalizedMessage disconnected();

        @DefaultMessage("Simulator agent channel is stopped.")
        ILocalizedMessage stopped();
    }
}
