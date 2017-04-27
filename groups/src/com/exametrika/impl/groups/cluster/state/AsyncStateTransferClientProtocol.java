/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.channel.IChannelReconnector;
import com.exametrika.impl.groups.cluster.discovery.GroupJoinMessagePart;
import com.exametrika.impl.groups.cluster.discovery.IGroupJoinStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.spi.groups.IAsyncStateStore;
import com.exametrika.spi.groups.IStateTransferFactory;

/**
 * The {@link AsyncStateTransferClientProtocol} represents a state transfer client protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class AsyncStateTransferClientProtocol extends AbstractProtocol implements IFailureDetectionListener,
    IFlushParticipant, IGroupJoinStrategy
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipManager membershipManager;
    private final IStateTransferFactory stateTransferFactory;
    private final UUID groupId;
    private final IAsyncStateStore stateStore;
    private ICompartment compartment;
    private IChannelReconnector channelReconnector;
    private final ISerializationRegistry serializationRegistry;
    private final long maxStateTransferPeriod;
    private final long stateSizeThreshold;
    private final Random random = new Random();
    private IFlush flush;
    private boolean joined;
    private boolean joining;
    private StoreStateLoadTask stateLoadTask;
    private StateTransfer stateTransfer;
    private List<IAddress> healthyMembers;
    private long startTransferTime;
    private boolean transferred;
    private boolean discovered;

    public AsyncStateTransferClientProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipManager membershipManager, 
        IStateTransferFactory stateTransferFactory, UUID groupId,
        ISerializationRegistry serializationRegistry, long maxStateTransferPeriod, long stateSizeThreshold)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(stateTransferFactory);
        Assert.notNull(serializationRegistry);
        Assert.notNull(groupId);
        
        this.membershipManager = membershipManager;
        this.stateTransferFactory = stateTransferFactory;
        this.groupId = groupId;
        this.stateStore = (IAsyncStateStore)stateTransferFactory.createStore(groupId);
        this.maxStateTransferPeriod = maxStateTransferPeriod;
        this.stateSizeThreshold = stateSizeThreshold;
        this.serializationRegistry = serializationRegistry;
    }

    public void setCompartment(ICompartment compartment)
    {
        Assert.notNull(compartment);
        Assert.isNull(this.compartment);
        
        this.compartment = compartment;
    }
    
    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        Assert.notNull(channelReconnector);
        Assert.isNull(this.channelReconnector);
        
        this.channelReconnector = channelReconnector;
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        Assert.checkState(joined);
        
        if (stateTransfer != null && stateTransfer.server.equals(member.getAddress()))
        {
            healthyMembers.remove(stateTransfer.server);
            updateStateTransfer();
            
            if (stateTransfer == null)
                channelReconnector.reconnect();
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
        Assert.notNull(healthyMembers);
        Assert.isTrue(!healthyMembers.isEmpty());
        Assert.checkState(!joined);
        
        this.healthyMembers = healthyMembers;
        discovered = true;
        
        updateStateTransfer();
        
        if (joining)
            send(messageFactory.create(healthyMembers.get(0), new GroupJoinMessagePart(membershipManager.getLocalNode())));
    }

    @Override
    public void onGroupFailed()
    {
        Assert.checkState(!joined);
        
        healthyMembers = null;
        discovered = false;
        updateStateTransfer();
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
        this.flush = flush;
        joined = true;
        flush.grantFlush(this);
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
            Assert.checkState(stateTransfer == null);
            
            if (stateLoadTask != null)
                stateLoadTask.cancel();
            
            IGroup group = flush.getNewMembership().getGroup();
            stateLoadTask = new StoreStateLoadTask(stateTransferFactory, stateStore, 
                group.getId(), group.getName(), marker, new ICompletionHandler() 
            {
                @Override
                public void onSucceeded(Object value)
                {
                    stateLoadTask = null;
                    flush.grantFlush(AsyncStateTransferClientProtocol.this);
                }
                
                @Override
                public void onFailed(Throwable error)
                {
                    stateLoadTask = null;
                    channelReconnector.reconnect();
                }
            });
            compartment.execute(stateLoadTask);
        }
        else if (transferred)
            flush.grantFlush(this);
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
        registry.register(new StateTransferResponseMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(StateTransferResponseMessagePartSerializer.ID);
    }

    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        if (joined)
            return;
        
        if (stateTransfer != null && cleanupManager.canCleanup(stateTransfer.server))
        {
            healthyMembers.remove(stateTransfer.server);
            updateStateTransfer();
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, final IMessage message)
    {
        if (message.getPart() instanceof StateTransferResponseMessagePart)
        {
            StateTransferResponseMessagePart part = message.getPart();
            if (stateTransfer == null || !stateTransfer.server.equals(message.getSource()))
            {
                compartment.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (message.getFiles() != null)
                        {
                            for (File file : message.getFiles())
                                file.delete();
                        }
                    }
                });
                return;
            }
                
            if (part.isRejected())
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.stateTransferRejected(stateTransfer.server));
                
                if (discovered)
                    healthyMembers.add(stateTransfer.server);
                stateTransfer.cancel();
                stateTransfer = null;
                updateStateTransfer();
                return;
            }
            
            if (message.getFiles() == null)
            {
                Assert.isTrue(part.isLast());
                
                stateTransfer.completed();
                return;
            }
            
            if (part.isFirst())
            {
                Assert.isTrue(message.getFiles().size() == 1);
                File file = message.getFiles().get(0);

                stateTransfer.loadSnapshot(file, part.isLast());
                
                if (canSwitchToSyncPhase(file.length()))
                {
                    joining = true;
                    send(messageFactory.create(healthyMembers.get(0), new GroupJoinMessagePart(membershipManager.getLocalNode())));
                }
            }
            else
            {
                Assert.isTrue(message.getFiles().size() == 1);
                File file = message.getFiles().get(0);
                
                stateTransfer.addFile(file, part.isLast());
                
                if (!joining && canSwitchToSyncPhase(file.length()))
                {
                    joining = true;
                    send(messageFactory.create(healthyMembers.get(0), new GroupJoinMessagePart(membershipManager.getLocalNode())));
                }
            }
        }
        else
            receiver.receive(message);
    }

    private void updateStateTransfer()
    {
        if (stateTransfer != null)
        {
            if (healthyMembers != null && healthyMembers.contains(stateTransfer.server))
                return;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stateTransferCanceled(stateTransfer.server));
            
            stateTransfer.cancel();
            stateTransfer = null;
            joining = false;
        }
        
        if (healthyMembers == null || healthyMembers.isEmpty())
            return;
        
        stateTransfer = new StateTransfer(healthyMembers.remove(random.nextInt(healthyMembers.size())));
        send(messageFactory.create(stateTransfer.server, MessageFlags.STATE_TRANSFER_REQUEST));
        startTransferTime = timeService.getCurrentTime();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stateTransferStarted(stateTransfer.server));
    }
    
    private boolean canSwitchToSyncPhase(long stateSize)
    {
        if (stateSize <= stateSizeThreshold)
            return true;
        if (timeService.getCurrentTime() - startTransferTime > maxStateTransferPeriod)
            return true;
        
        return false;
    }

    private class StateTransfer implements ICompletionHandler
    {
        private final IAddress server;
        private SnapshotLoadTask loadSnapshotTask;
        private MessagesLoadTask loadMessagesTask;
        private boolean snapshotLoaded;
        private List<File> messagesFiles = new ArrayList<File>();
        private boolean last;
        
        public StateTransfer(IAddress server)
        {
            this.server = server;
        }
        
        public void loadSnapshot(File snapshotFile, boolean last)
        {
            Assert.checkState(loadSnapshotTask == null && !snapshotLoaded);
            Assert.checkState(!last || flush != null);
            
            this.last = last;
            loadSnapshotTask = new SnapshotLoadTask(stateTransferFactory, snapshotFile, this, groupId);
            compartment.execute(loadSnapshotTask);
        }
        
        public void addFile(File messagesFile, boolean last)
        {
            Assert.checkState(loadSnapshotTask != null || snapshotLoaded);
            Assert.checkState(!last || flush != null);
            
            this.last = last;
            messagesFiles.add(messagesFile);
            
            if (snapshotLoaded && loadMessagesTask == null)
            {
                loadMessagesTask = new MessagesLoadTask(getReceiver(), messagesFiles, compartment, this, serializationRegistry);
                messagesFiles = new ArrayList<File>();
                compartment.execute(loadMessagesTask);
            }
        }
        
        public void cancel()
        {
            if (loadSnapshotTask != null)
                loadSnapshotTask.cancel();
            if (loadMessagesTask != null)
                loadMessagesTask.cancel();
            
            if (!messagesFiles.isEmpty())
            {
                final List<File> messagesFiles = this.messagesFiles;
                this.messagesFiles = new ArrayList<File>();
                compartment.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (File file : messagesFiles)
                            file.delete();
                    }
                });
            }
        }

        public void completed()
        {
            Assert.checkState(flush != null);
            
            last = true;
            if (loadSnapshotTask == null && loadMessagesTask == null)
            {
                Assert.checkState(messagesFiles.isEmpty());
                
                flush.grantFlush(AsyncStateTransferClientProtocol.this);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.stateTransferCompleted(server));
            }
        }
        
        @Override
        public void onSucceeded(Object value)
        {
            loadSnapshotTask = null;
            loadMessagesTask = null;
            snapshotLoaded = true;
            
            if (!messagesFiles.isEmpty())
            {
                loadMessagesTask = new MessagesLoadTask(getReceiver(), messagesFiles, compartment, this, serializationRegistry);
                messagesFiles = new ArrayList<File>();
                compartment.execute(loadMessagesTask);
            }
            else if (last)
                completed();
        }

        @Override
        public void onFailed(Throwable error)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, error);
            
            loadSnapshotTask = null;
            loadMessagesTask = null;
            cancel();
            channelReconnector.reconnect();
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("State transfer from ''{0}'' has been completed.")
        ILocalizedMessage stateTransferCompleted(IAddress server);

        @DefaultMessage("State transfer from ''{0}'' has been started.")
        ILocalizedMessage stateTransferStarted(IAddress server);

        @DefaultMessage("State transfer from ''{0}'' has been canceled.")
        ILocalizedMessage stateTransferCanceled(IAddress server);
        
        @DefaultMessage("State transfer from ''{0}'' has been rejected by server.")
        ILocalizedMessage stateTransferRejected(IAddress server);
    }
}
