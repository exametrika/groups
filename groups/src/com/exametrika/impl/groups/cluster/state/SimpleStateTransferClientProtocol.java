/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.discovery.GroupJoinMessagePart;
import com.exametrika.impl.groups.cluster.discovery.IGroupJoinStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.spi.groups.ISimpleStateStore;
import com.exametrika.spi.groups.ISimpleStateTransferClient;
import com.exametrika.spi.groups.ISimpleStateTransferFactory;

/**
 * The {@link SimpleStateTransferClientProtocol} represents a simple state transfer client protocol, which keeps state in memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleStateTransferClientProtocol extends AbstractProtocol implements IFailureDetectionListener,
    IFlushParticipant, IGroupJoinStrategy
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipManager membershipManager;
    private final ISimpleStateTransferClient client;
    private final ISimpleStateStore stateStore;
    private final Random random = new Random();
    private List<IAddress> healthyMembers = new ArrayList<IAddress>();
    private IAddress server;
    private IFlush flush;
    private boolean transferred;

    public SimpleStateTransferClientProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipManager membershipManager, 
        ISimpleStateTransferFactory stateTransferFactory, ISimpleStateStore stateStore)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(stateTransferFactory);
        Assert.notNull(stateStore);
        
        this.membershipManager = membershipManager;
        this.client = stateTransferFactory.createClient();
        this.stateStore = stateStore;
    }

    @Override
    public void onMemberFailed(INode member)
    {
        if (transferred)
            return;
        
        if (member.getAddress().equals(server))
        {
            healthyMembers.remove(member);
            updateStateTransfer();
        }
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }

    @Override
    public void onGroupDiscovered(List<IAddress> healthyMembers)
    {
        this.healthyMembers = healthyMembers;
        send(messageFactory.create(healthyMembers.get(0), new GroupJoinMessagePart(membershipManager.getLocalNode())));
    }

    @Override
    public void onGroupFailed()
    {
        healthyMembers.clear();
        server = null;
    }
    
    @Override
    public boolean isFlushProcessingRequired()
    {
        return true;
    }

    @Override
    public void setCoordinator()
    {
    }
    
    @Override
    public void startFlush(IFlush flush)
    {
        flush.grantFlush(this);
        this.flush = flush;
    }

    @Override
    public void beforeProcessFlush()
    {
    }
    
    @Override
    public void processFlush()
    {
        if (flush.isGroupForming())
        {
            IGroup group = flush.getNewMembership().getGroup();
            ByteArray state = stateStore.load(group.getId());
            if (state != null)
                client.loadSnapshot(false, state);
            else  if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, marker, messages.stateUnavailable(group.getName()));
            
            flush.grantFlush(this);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.snapshotLoaded());
        }
        else if (transferred)
            flush.grantFlush(this);
        else
            updateStateTransfer();
    }

    @Override
    public void endFlush()
    {
       flush = null;
       transferred = true;
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new SimpleStateTransferResponseMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(SimpleStateTransferResponseMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, final IMessage message)
    {
        if (message.getPart() instanceof SimpleStateTransferResponseMessagePart)
        {
            Assert.notNull(flush);
            Assert.isTrue(!transferred);
            Assert.isTrue(server.equals(message.getSource()));
            
            SimpleStateTransferResponseMessagePart part = message.getPart();
            client.loadSnapshot(true, part.getState());
            transferred = true;
            server = null;
            healthyMembers.clear();
            
            flush.grantFlush(this);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stateTransferCompleted(server));
        }
        else
            receiver.receive(message);
    }
    
    private void updateStateTransfer()
    {
        if (transferred)
            return;
        
        if (server != null)
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stateTransferFailed(server));
        }
        
        server = null;
        if (healthyMembers.isEmpty())
            return;
        
        server = healthyMembers.remove(random.nextInt(healthyMembers.size()));
        send(messageFactory.create(server, MessageFlags.STATE_TRANSFER_REQUEST));
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stateTransferStarted(server));
    }
    
    private interface IMessages
    {
        @DefaultMessage("State transfer from ''{0}'' has been completed.")
        ILocalizedMessage stateTransferCompleted(IAddress server);
        
        @DefaultMessage("State transfer from ''{0}'' has been started.")
        ILocalizedMessage stateTransferStarted(IAddress server);
        
        @DefaultMessage("State transfer from ''{0}'' has been failed.")
        ILocalizedMessage stateTransferFailed(IAddress server);
        
        @DefaultMessage("Snapshot has been loaded from external storage.")
        ILocalizedMessage snapshotLoaded();

        @DefaultMessage("Requested state of group ''{0}'' is not available in state store.")
        ILocalizedMessage stateUnavailable(String group);
    }
}
