/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;

/**
 * The {@link WorkerClusterMembershipProtocol} represents a worker node part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerClusterMembershipProtocol extends AbstractClusterMembershipProtocol
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<IWorkerControllerObserver> controllerObservers;
    private IAddress controller;
    
    public WorkerClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders, List<IWorkerControllerObserver> controllerObservers)
    {
        super(channelName, messageFactory, membershipManager, membershipProviders);
        
        Assert.notNull(controllerObservers);
        
        this.controllerObservers = controllerObservers;
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof ClusterMembershipMessagePart)
        {
            ClusterMembershipMessagePart part = message.getPart();
            installMembership(part);
            
            send(messageFactory.create(message.getSource(), new ClusterMembershipResponseMessagePart(part.getRoundId())));
            
            if (!message.getSource().equals(controller))
            {
                controller = message.getSource();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.controllerChanged(controller));
                
                for (IWorkerControllerObserver controllerObserver : controllerObservers)
                    controllerObserver.onControllerChanged(controller);
            }
        }
        else
            receiver.receive(message);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Controller has been changed: {0}.")
        ILocalizedMessage controllerChanged(IAddress controller);
    }
}
