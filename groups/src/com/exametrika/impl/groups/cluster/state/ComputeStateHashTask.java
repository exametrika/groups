/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.io.IOException;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;

/**
 * The {@link ComputeStateHashTask} represents a task which computes MD5 hash of state.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ComputeStateHashTask implements ICompartmentTask
{
    private final ICompletionHandler completionHandler;
    private final IAsyncStateTransferServer server; 
    private boolean canceled;

    public ComputeStateHashTask(IAsyncStateTransferServer stateTransferServer, ICompletionHandler completionHandler)
    {
        Assert.notNull(stateTransferServer);
        Assert.notNull(completionHandler);
        
        this.completionHandler = completionHandler;
        this.server = stateTransferServer;
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
            
            server.saveSnapshot(true, file);
            
            return Files.md5Hash(file);
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