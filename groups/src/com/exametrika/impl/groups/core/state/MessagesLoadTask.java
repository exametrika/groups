/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.state;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;

/**
 * The {@link MessagesLoadTask} is task which loads messages from specified file.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessagesLoadTask implements ICompartmentTask
{
    private final IReceiver receiver;
    private final List<File> files;
    private final ICompletionHandler completionHandler;
    private final ISerializationRegistry serializationRegistry;
    private final ICompartment compartment;
    private volatile boolean canceled;

    public MessagesLoadTask(IReceiver receiver, List<File> files, ICompartment compartment, ICompletionHandler completionHandler,
        ISerializationRegistry serializationRegistry)
    {
        Assert.notNull(receiver);
        Assert.notNull(files);
        Assert.notNull(compartment);
        Assert.notNull(completionHandler);
        Assert.notNull(serializationRegistry);
        
        this.receiver = receiver;
        this.files = files;
        this.compartment = compartment;
        this.completionHandler = completionHandler;
        this.serializationRegistry = serializationRegistry;
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
            for (File file : files)
            {
                if (canceled)
                    break;
                
                final List<IMessage> messages = new ArrayList<IMessage>();
                StateTransferMessageLog.load(file, messages, serializationRegistry);
                
                RunnableFuture future = new FutureTask(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (IMessage message : messages)
                        {
                            if (canceled)
                                break;
                            
                            receiver.receive(message);
                        }
                    }
                }, null);

                compartment.offer(future);
                future.get();
            }
        }
        catch (CancellationException e)
        {
        }
        catch (Exception e)
        {
            throw new ChannelException(e);
        }
        finally
        {
            for (File file : files)
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