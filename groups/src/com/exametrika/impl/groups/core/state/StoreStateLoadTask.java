/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;

/**
 * The {@link StoreStateLoadTask} is a task which loads state from external state store.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StoreStateLoadTask implements ICompartmentTask
{
    private final UUID groupId;
    private final IStateTransferFactory stateTransferFactory;
    private final IStateStore stateStore;
    private final ICompletionHandler completionHandler;
    private boolean canceled;

    public StoreStateLoadTask(IStateTransferFactory stateTransferFactory, IStateStore stateStore, UUID groupId, ICompletionHandler completionHandler)
    {
        Assert.notNull(stateTransferFactory);
        Assert.notNull(stateStore);
        Assert.notNull(groupId);
        Assert.notNull(completionHandler);
        
        this.stateTransferFactory = stateTransferFactory;
        this.stateStore = stateStore;
        this.groupId = groupId;
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
            stateStore.load(groupId, file);
            
            IStateTransferClient client = stateTransferFactory.createClient();
            client.loadSnapshot(file);
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