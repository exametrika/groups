/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.message;

import java.io.File;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MessageFactory} is an implementation of {@link IMessageFactory}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageFactory implements IMessageFactory
{
    private final ISerializationRegistry serializationRegistry;
    private final ILiveNodeProvider liveNodeProvider;

    public MessageFactory(ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider)
    {
        Assert.notNull(serializationRegistry);
        Assert.notNull(liveNodeProvider);
        
        this.serializationRegistry = serializationRegistry;
        this.liveNodeProvider = liveNodeProvider;
    }
    
    @Override
    public IMessage create(IAddress destination, IMessagePart part)
    {
        Assert.notNull(destination);
        Assert.notNull(part);
        return new Message(liveNodeProvider.getLocalNode(), destination, part, 0, null,
            serializationRegistry);
    }

    @Override
    public IMessage create(IAddress destination, int flags)
    {
        Assert.notNull(destination);
        Assert.notNull(flags);
        return new Message(liveNodeProvider.getLocalNode(), destination, flags, serializationRegistry);
    }
    
    @Override
    public IMessage create(IAddress destination, IMessagePart part, int flags)
    {
        Assert.notNull(destination);
        Assert.notNull(part);
        Assert.notNull(flags);
        return new Message(liveNodeProvider.getLocalNode(), destination, part, flags, null, serializationRegistry);
    }
    
    @Override
    public IMessage create(IAddress destination, IMessagePart part, int flags, List<File> files)
    {
        Assert.notNull(destination);
        Assert.notNull(part);
        Assert.notNull(flags);
        return new Message(liveNodeProvider.getLocalNode(), destination, part, flags, files, serializationRegistry);
    }
}
