/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.groups.core.INode;
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
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.multicast.QueueCapacityController;
import com.exametrika.impl.groups.core.multicast.RemoteFlowId;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.IStateTransferServer;

/**
 * The {@link StateTransferServerProtocol} represents a state transfer server protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class StateTransferServerProtocol extends AbstractProtocol implements IFlushParticipant
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;
    private final IStateStore stateStore;
    private ICompartment compartment;
    private final ISerializationRegistry serializationRegistry;
    private final long saveSnapshotPeriod;
    private StateTransfer stateTransfer;
    private StoreStateSaveTask stateSaveTask;
    private long lastSaveSnapshotTime;
    private boolean snapshotChanged;
    private IFlush flush;
    private boolean processing;
    private List<IMessage> pendingMessages;
    private final long transferLogRecordPeriod;
    private final int transferLogMessagesCount;
    private final IStateTransferServer server;
    private boolean snapshotRequest;
    private final QueueCapacityController capacityController;
    
    public StateTransferServerProtocol(String channelName, IMessageFactory messageFactory, IMembershipManager membershipManager, 
        IFailureDetector failureDetector, IStateTransferFactory stateTransferFactory, IStateStore stateStore,
        ISerializationRegistry serializationRegistry,
        long saveSnapshotPeriod, long transferLogRecordPeriod, int transferLogMessagesCount, int minLockQueueCapacity)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(stateTransferFactory);
        Assert.notNull(stateStore);
        Assert.notNull(serializationRegistry);
        
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.stateStore = stateStore;
        this.saveSnapshotPeriod = saveSnapshotPeriod;
        this.transferLogRecordPeriod = transferLogRecordPeriod;
        this.transferLogMessagesCount = transferLogMessagesCount;
        this.serializationRegistry = serializationRegistry;
        this.server = stateTransferFactory.createServer();
        this.capacityController = new QueueCapacityController(minLockQueueCapacity, 0, Memberships.CORE_GROUP_ADDRESS, 
            Memberships.CORE_GROUP_ID);
    }

    public void setCompartment(ICompartment compartment)
    {
        Assert.notNull(compartment);
        Assert.isNull(this.compartment);
        
        this.compartment = compartment;
    }
    
    public void setFlowController(IFlowController<RemoteFlowId> flowController)
    {
        capacityController.setFlowController(flowController);
    }
    
    @Override
    public void start()
    {
        this.lastSaveSnapshotTime = timeService.getCurrentTime();
        super.start();
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        INode coordinator = failureDetector.getCurrentCoordinator();
        if (coordinator == null)
            return;
        
        if (flush == null && stateSaveTask == null && stateTransfer == null && 
            snapshotChanged && timeService.getCurrentTime() >= lastSaveSnapshotTime + saveSnapshotPeriod &&
            membershipManager.getLocalNode().equals(coordinator))
        {        
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.snapshotStarted());
            
            stateSaveTask = new StoreStateSaveTask(server, stateStore, 
                membershipManager.getPreparedMembership().getGroup().getId(), new ICompletionHandler()
            {
                @Override
                public void onSucceeded(Object value)
                {
                    stateSaveTask = null;
                    lastSaveSnapshotTime = timeService.getCurrentTime();
                    snapshotChanged = false;
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.snapshotSucceeded());
                    
                    deliverPendingMessages();
                }
                
                @Override
                public void onFailed(Throwable error)
                {
                    stateSaveTask = null;
                    lastSaveSnapshotTime = timeService.getCurrentTime();
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.snapshotFailed());
                    
                    deliverPendingMessages();
                }
            });
            compartment.execute(stateSaveTask);
        }
        
        if (stateTransfer != null)
            stateTransfer.tryMakeBundle();
    }

    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        if (stateTransfer != null && cleanupManager.canCleanup(stateTransfer.client))
        {
            stateTransfer.cancel();
            stateTransfer = null;
        }
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
        
        if ((stateSaveTask == null && (stateTransfer == null || stateTransfer.saveSnapshotTask == null)))
        {
            Assert.checkState(pendingMessages == null);
            flush.grantFlush(this);
        }
    }

    @Override
    public void beforeProcessFlush()
    {
    }
    
    @Override
    public void processFlush()
    {
        if (snapshotRequest)
        {
            Assert.notNull(stateTransfer);
            
            snapshotRequest = false;
            stateTransfer.saveSnapshot();
        }
        else if (stateTransfer != null)
            stateTransfer.tryMakeBundle();
        
        flush.grantFlush(this);
        processing = true;
    }

    @Override
    public void endFlush()
    {
        flush = null;
        processing = false;
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.hasFlags(MessageFlags.STATE_TRANSFER_REQUEST))
        {
            if (stateTransfer != null || stateSaveTask != null)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.stateTransferRejected(message.getSource()));
                
                send(messageFactory.create(message.getSource(), new StateTransferResponseMessagePart(false, false, true)));
                return;
            }
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stateTransferStarted(message.getSource()));
            
            stateTransfer = new StateTransfer(message.getSource());
            if (flush != null && !processing)
                snapshotRequest = true;
            else
                stateTransfer.saveSnapshot();
        }
        else if ((stateSaveTask != null || (stateTransfer != null && stateTransfer.saveSnapshotTask != null)))
        {
            if (server.classifyMessage(message.getPart()) != IStateTransferServer.MessageType.NON_STATE)
                addPendingMessage(message);
            else
                receiver.receive(message);
        }
        else
        {
            if (!snapshotRequest && server.classifyMessage(message.getPart()) == IStateTransferServer.MessageType.STATE_WRITE)
            {
                if (stateTransfer != null)
                    stateTransfer.addMessage(message);
                
                snapshotChanged = true;
            }
            
            receiver.receive(message);
        }
    }

    private void addPendingMessage(IMessage message)
    {
        if (pendingMessages == null)
            pendingMessages = new ArrayList<IMessage>();
        
        pendingMessages.add(message);
        capacityController.addCapacity(message.getSource(), message.getSize());
    }

    private void deliverPendingMessages()
    {
        if (pendingMessages != null)
        {
            snapshotChanged = true;
            
            for (IMessage message : pendingMessages)
                receive(message);
            
            pendingMessages = null;
            capacityController.clearCapacity();
        }
        
        if (flush != null && !processing)
            flush.grantFlush(this);
    }
    
    private class StateTransfer implements ICompletionHandler
    {
        private final IAddress client;
        private SnapshotSaveTask saveSnapshotTask;
        private MessagesSaveTask saveMessagesTask;
        private boolean snapshotSaved;
        private List<IMessage> messages = new ArrayList<IMessage>();
        private long startRecordTime;
        
        public StateTransfer(IAddress client)
        {
            this.client = client;
        }
        
        public void saveSnapshot()
        {
            Assert.checkState(saveSnapshotTask == null && !snapshotSaved);
            saveSnapshotTask = new SnapshotSaveTask(server, this);
            compartment.execute(saveSnapshotTask);
        }
        
        public void addMessage(IMessage message)
        {
            Assert.isTrue(message.getFiles() == null);
            Assert.checkState(saveSnapshotTask != null || snapshotSaved);
            
            if (messages.isEmpty())
                startRecordTime = timeService.getCurrentTime();
            
            messages.add(message);
            
            tryMakeBundle();
        }
        
        public void cancel()
        {
            if (saveSnapshotTask != null)
            {
                saveSnapshotTask.cancel();
                saveSnapshotTask = null;
                deliverPendingMessages();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, StateTransferServerProtocol.messages.stateTransferCanceled(client));
            }
            
            if (saveMessagesTask != null)
                saveMessagesTask.cancel();
        }
        
        @Override
        public void onSucceeded(Object value)
        {
            boolean last;
            if (processing && messages.isEmpty() && flush.getNewMembership().getGroup().findMember(client) != null)
                last = true;
            else
                last = false;
            
            if (saveSnapshotTask != null)
            {
                send(messageFactory.create(client, new StateTransferResponseMessagePart(true, last, false), 
                    MessageFlags.LOW_PRIORITY, Collections.singletonList((File)value)));
                
                saveSnapshotTask = null;
                snapshotSaved = true;
                
                deliverPendingMessages();
            }
            
            if (saveMessagesTask != null)
            {
                send(messageFactory.create(client, new StateTransferResponseMessagePart(false, last, false), 
                    MessageFlags.LOW_PRIORITY, Collections.singletonList((File)value)));
                
                saveMessagesTask = null;
            }
            
            if (!messages.isEmpty())
                tryMakeBundle();
            else if (processing)
                stateTransfer = null;
        }

        @Override
        public void onFailed(Throwable error)
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, error);
            
            send(messageFactory.create(client, new StateTransferResponseMessagePart(false, false, true)));
            cancel();
            stateTransfer = null;
        }
        
        private boolean tryMakeBundle()
        {
            if (!snapshotSaved || saveMessagesTask != null)
                return false;
            if (messages.size() <= transferLogMessagesCount && timeService.getCurrentTime() <= startRecordTime + transferLogRecordPeriod)
                return false;
            
            saveMessagesTask = new MessagesSaveTask(messages, this, serializationRegistry);
            messages = new ArrayList<IMessage>();
            compartment.execute(saveMessagesTask);
            return true;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("State transfer to ''{0}'' has been completed.")
        ILocalizedMessage stateTransferCompleted(IAddress client);

        @DefaultMessage("State transfer to ''{0}'' has been started.")
        ILocalizedMessage stateTransferStarted(IAddress client);

        @DefaultMessage("State transfer to ''{0}'' has been canceled.")
        ILocalizedMessage stateTransferCanceled(IAddress client);
        
        @DefaultMessage("State transfer to ''{0}'' has been rejected by server.")
        ILocalizedMessage stateTransferRejected(IAddress client);
        
        @DefaultMessage("Snapshot to external storage has been started.")
        ILocalizedMessage snapshotStarted();
        
        @DefaultMessage("Snapshot to external storage has succeeded.")
        ILocalizedMessage snapshotSucceeded();
        
        @DefaultMessage("Snapshot to external storage has failed.")
        ILocalizedMessage snapshotFailed();
    }
}
