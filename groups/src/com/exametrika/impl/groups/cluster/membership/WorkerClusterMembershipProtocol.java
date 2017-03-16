/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;

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
    private final IWorkerControllerObserver controllerObserver;
    private IAddress controller;
    
    public WorkerClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipManager membershipManager,
        List<IClusterMembershipProvider> membershipProviders, IWorkerControllerObserver controllerObserver)
    {
        super(channelName, messageFactory, membershipManager, membershipProviders);
        
        Assert.notNull(controllerObserver);
        
        this.controllerObserver = controllerObserver;
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
                controllerObserver.onControllerChanged(controller);
            }
        }
        else
            receiver.receive(message);
    }
}
