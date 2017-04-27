/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.IAsyncStateStore;
import com.exametrika.spi.groups.IAsyncStateTransferServer;

/**
 * The {@link StoreStateSaveTask} represents a task which saves state to external state store.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StoreStateSaveTask implements ICompartmentTask
{
    private final UUID groupId;
    private final IAsyncStateStore stateStore;
    private final ICompletionHandler completionHandler;
    private final IAsyncStateTransferServer server; 
    private boolean canceled;

    public StoreStateSaveTask(IAsyncStateTransferServer stateTransferServer, IAsyncStateStore stateStore, UUID groupId, ICompletionHandler completionHandler)
    {
        Assert.notNull(stateTransferServer);
        Assert.notNull(stateStore);
        Assert.notNull(groupId);
        Assert.notNull(completionHandler);
        
        this.stateStore = stateStore;
        this.groupId = groupId;
        this.completionHandler = completionHandler;
        this.server = stateTransferServer;
    }
    
    public IAsyncStateTransferServer getServer()
    {
        return server;
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
            
            server.saveSnapshot(false, file);
            
            stateStore.save(groupId, file);
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
}