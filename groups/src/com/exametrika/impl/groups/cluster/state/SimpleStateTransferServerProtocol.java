/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
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
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

/**
 * The {@link SimpleStateTransferServerProtocol} represents a simple state transfer server protocol, which keeps state in memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SimpleStateTransferServerProtocol extends AbstractProtocol implements IFlushParticipant
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipManager membershipManager;
    private final IGroupFailureDetector failureDetector;
    private final ISimpleStateTransferServer server;
    private final ISimpleStateStore stateStore;
    private final long saveSnapshotPeriod;
    private long lastSaveSnapshotTime;
    private boolean snapshotChanged;
    private IFlush flush;
    
    public SimpleStateTransferServerProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipManager membershipManager, 
        IGroupFailureDetector failureDetector, IStateTransferFactory stateTransferFactory, UUID groupId,
        long saveSnapshotPeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(stateTransferFactory);
        Assert.notNull(groupId);
        
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.server = (ISimpleStateTransferServer)stateTransferFactory.createServer(groupId);
        this.stateStore = (ISimpleStateStore)stateTransferFactory.createStore(groupId);
        this.saveSnapshotPeriod = saveSnapshotPeriod;
    }

    @Override
    public void onTimer(long currentTime)
    {
        INode coordinator = failureDetector.getCurrentCoordinator();
        if (coordinator == null)
            return;
        
        if (flush == null && snapshotChanged && timeService.getCurrentTime() >= lastSaveSnapshotTime + saveSnapshotPeriod &&
            membershipManager.getLocalNode().equals(coordinator))
        {        
            ByteArray state = server.saveSnapshot(false);
            stateStore.save(membershipManager.getPreparedMembership().getGroup().getId(), state);
            
            lastSaveSnapshotTime = timeService.getCurrentTime();
            snapshotChanged = false;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.snapshotSaved());
        }
    }

    @Override
    public boolean isFlushProcessingRequired()
    {
        return false;
    }

    @Override
    public void setCoordinator()
    {
    }

    @Override
    public void startFlush(IFlush flush)
    {
        this.flush = flush;
    }

    @Override
    public void beforeProcessFlush()
    {
    }

    @Override
    public void processFlush()
    {
    }

    @Override
    public void endFlush()
    {
        flush = null;
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.hasFlags(MessageFlags.STATE_TRANSFER_REQUEST))
        {
            ByteArray state = server.saveSnapshot(true);
                
            send(messageFactory.create(message.getSource(), new SimpleStateTransferResponseMessagePart(state)));
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stateSent(message.getSource()));
        }
        else
        {
            if (server.classifyMessage(message) == ISimpleStateTransferServer.MessageType.STATE_WRITE)
                snapshotChanged = true;
            
            receiver.receive(message);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("State has been sent to ''{0}''.")
        ILocalizedMessage stateSent(IAddress client);
        @DefaultMessage("Snapshot has been saved to external storage.")
        ILocalizedMessage snapshotSaved();
    }
}
