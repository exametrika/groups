/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
import java.io.IOException;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.groups.IStateTransferServer;

/**
 * The {@link SnapshotSaveTask} represents a task which saves state to temporal file.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SnapshotSaveTask implements ICompartmentTask<File>
{
    private final IStateTransferServer stateTransferServer;
    private final ICompletionHandler completionHandler;
    private boolean canceled;
    private File file;

    public SnapshotSaveTask(IStateTransferServer stateTransferServer, ICompletionHandler completionHandler)
    {
        Assert.notNull(stateTransferServer);
        Assert.notNull(completionHandler);
        
        this.stateTransferServer = stateTransferServer;
        this.completionHandler = completionHandler;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public void cancel()
    {
        canceled = true;
        
        if (file != null)
            file.delete();
    }
    
    @Override
    public File execute()
    {
        File file = null;
        try
        {
            file = File.createTempFile("groups-state", null);
            stateTransferServer.saveSnapshot(file);
            
            return file;
        }
        catch (IOException e)
        {
            throw new ChannelException(e);
        }
        catch (Exception e)
        {
            if (file != null)
                file.delete();
            
            return Exceptions.wrapAndThrow(e);
        }
    }

    @Override
    public void onSucceeded(File result)
    {
        if (!canceled)
        {
            file = result;
            completionHandler.onSucceeded(result);
        }
        else
            file.delete();
    }

    @Override
    public void onFailed(Throwable error)
    {
        if (!canceled)
            completionHandler.onFailed(error);
    }
}