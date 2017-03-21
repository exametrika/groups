/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.io.File;
import java.util.List;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.IOs;

/**
 * The {@link MessagesSaveTask} is task which saves messages to specified file.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessagesSaveTask implements ICompartmentTask<File>
{
    private final List<IMessage> messages;
    private final ICompletionHandler completionHandler;
    private final ISerializationRegistry serializationRegistry;
    private boolean canceled;

    public MessagesSaveTask(List<IMessage> messages, ICompletionHandler completionHandler, ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(messages);
        Assert.notNull(completionHandler);
        Assert.notNull(serializationRegistry);
        
        this.messages = messages;
        this.completionHandler = completionHandler;
        this.serializationRegistry = serializationRegistry;
    }
    
    public void cancel()
    {
        canceled = true;
    }
    
    @Override
    public File execute()
    {
        File file = null;
        StateTransferMessageLog log = null;
        try
        {
            file = File.createTempFile("groups-state", null);
            log = new StateTransferMessageLog(file, false);
            log.save(messages, serializationRegistry);
            
            return file;
        }
        catch (Exception e)
        {
            if (file != null)
                file.delete();
            
            throw new ChannelException(e);
        }
        finally
        {
            IOs.close(log);
        }
    }

    @Override
    public void onSucceeded(File result)
    {
        if (!canceled)
            completionHandler.onSucceeded(result);
        else
            result.delete();
    }

    @Override
    public void onFailed(Throwable error)
    {
        if (!canceled)
            completionHandler.onFailed(error);
    }
}