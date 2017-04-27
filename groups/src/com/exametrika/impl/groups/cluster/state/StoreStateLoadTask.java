/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.IAsyncStateStore;
import com.exametrika.spi.groups.IAsyncStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;

/**
 * The {@link StoreStateLoadTask} is a task which loads state from external state store.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StoreStateLoadTask implements ICompartmentTask
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(StoreStateLoadTask.class);
    private final UUID groupId;
    private final String groupName;
    private final IMarker marker;
    private final IStateTransferFactory stateTransferFactory;
    private final IAsyncStateStore stateStore;
    private final ICompletionHandler completionHandler;
    private boolean canceled;

    public StoreStateLoadTask(IStateTransferFactory stateTransferFactory, IAsyncStateStore stateStore, UUID groupId, 
        String groupName, IMarker marker, ICompletionHandler completionHandler)
    {
        Assert.notNull(stateTransferFactory);
        Assert.notNull(stateStore);
        Assert.notNull(groupId);
        Assert.notNull(groupName);
        Assert.notNull(marker);
        Assert.notNull(completionHandler);
        
        this.stateTransferFactory = stateTransferFactory;
        this.stateStore = stateStore;
        this.groupId = groupId;
        this.groupName = groupName;
        this.marker = marker;
        this.completionHandler = completionHandler;
    }
    
    public void cancel()
    {
        canceled = true;
    }
    
    @Override
    public Object execute()
    {
        File file = null;
        try
        {
            file = File.createTempFile("groups-state", null);
            if (stateStore.load(groupId, file))
            {
                IAsyncStateTransferClient client = (IAsyncStateTransferClient)stateTransferFactory.createClient(groupId);
                client.loadSnapshot(false, file);
            }
            else if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, marker, messages.stateUnavailable(groupName));
        }
        catch (IOException e)
        {
            throw new ChannelException(e);
        }
        finally
        {
            if (file != null)
                file.delete();
        }

        return null;
    }

    @Override
    public void onSucceeded(Object result)
    {
        if (!canceled)
            completionHandler.onSucceeded(result);
    }

    @Override
    public void onFailed(Throwable error)
    {
        if (!canceled)
            completionHandler.onFailed(error);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Requested state of group ''{0}'' is not available in state store.")
        ILocalizedMessage stateUnavailable(String group);
    }
}