/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.management;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;

/**
 * The {@link CommandManager} is an implementation of group command manager.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CommandManager extends AbstractProtocol implements ICommandManager, IDeliveryHandler
{
    private final GroupAddress groupAddress;
    private final ICompartment compartment;
    private final List<ICommandHandler> commandHandlers;
    private final Deque<CommandTask> commands = new ArrayDeque<CommandTask>();

    public CommandManager(String channelName, IMessageFactory messageFactory, GroupAddress groupAddress, ICompartment compartment,
        List<ICommandHandler> commandHandlers)
    {
        super(channelName, null, messageFactory);
        
        Assert.notNull(groupAddress);
        Assert.notNull(compartment);
        Assert.notNull(commandHandlers);
        
        this.groupAddress = groupAddress;
        this.compartment = compartment;
        this.commandHandlers = commandHandlers;
    }
    
    @Override
    public void execute(ICommand command, ICompletionHandler<ICommand> completionHandler)
    {
        compartment.offer(new CommandTask(command, completionHandler));
    }

    @Override
    public void onDelivered(IMessage message)
    {
        if (message.getPart() instanceof CommandMessagePart)
        {
            CommandMessagePart part = message.getPart();
            CommandTask task = commands.removeFirst();
            
            Assert.checkState(task.command == part.getCommand());
            
            if (task.completionHandler != null)
                task.completionHandler.onSucceeded(task.command);
        }
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new CommandMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(CommandMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof CommandMessagePart)
        {
            CommandMessagePart part = message.getPart();
            ICommand command = part.getCommand();
            for (ICommandHandler commandHandler : commandHandlers)
            {
                if (commandHandler.supports(command))
                    commandHandler.execute(command);
            }
        }
        else
            receiver.receive(message);
    }
    
    private class CommandTask implements Runnable
    {
        private final ICommand command;
        private final ICompletionHandler<ICommand> completionHandler;

        public CommandTask(ICommand command, ICompletionHandler<ICommand> completionHandler)
        {
            Assert.notNull(command);
            
            this.command = command;
            this.completionHandler = completionHandler;
        }

        @Override
        public void run()
        {
            commands.addLast(this);
            
            send(messageFactory.create(groupAddress, new CommandMessagePart(command)));
        }
    }
}
