/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ICompletionHandler;

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
    private File file;

    public MessagesSaveTask(List<IMessage> messages, ICompletionHandler completionHandler, ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(messages);
        Assert.notNull(completionHandler);
        Assert.notNull(serializationRegistry);
        
        this.messages = messages;
        this.completionHandler = completionHandler;
        this.serializationRegistry = serializationRegistry;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public void cancel()
    {
        canceled = true;
    }
    
    @Override
    public File execute()
    {
        File file = null;
        try
        {
            file = File.createTempFile("groups-state", null);
            StateTransferMessageLog.save(messages, file, serializationRegistry);
            
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
            result.delete();
    }

    @Override
    public void onFailed(Throwable error)
    {
        if (!canceled)
            completionHandler.onFailed(error);
    }
}