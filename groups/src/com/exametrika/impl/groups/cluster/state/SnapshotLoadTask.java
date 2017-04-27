/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.util.UUID;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.IAsyncStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;

/**
 * The {@link SnapshotLoadTask} is task which loads state snapshot from specified file.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SnapshotLoadTask implements ICompartmentTask
{
    private final IStateTransferFactory stateTransferFactory;
    private final File file;
    private final ICompletionHandler completionHandler;
    private final UUID groupId;
    private boolean canceled;

    public SnapshotLoadTask(IStateTransferFactory stateTransferFactory, File file, ICompletionHandler completionHandler,
        UUID groupId)
    {
        Assert.notNull(stateTransferFactory);
        Assert.notNull(file);
        Assert.notNull(completionHandler);
        Assert.notNull(groupId);
        
        this.stateTransferFactory = stateTransferFactory;
        this.file = file;
        this.completionHandler = completionHandler;
        this.groupId = groupId;
    }
    
    public void cancel()
    {
        canceled = true;
    }
    
    @Override
    public Object execute()
    {

        try
        {
            IAsyncStateTransferClient client = (IAsyncStateTransferClient)stateTransferFactory.createClient(groupId);
            client.loadSnapshot(true, file);
        }
        catch (Exception e)
        {
            throw new ChannelException(e);
        }
        finally
        {
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