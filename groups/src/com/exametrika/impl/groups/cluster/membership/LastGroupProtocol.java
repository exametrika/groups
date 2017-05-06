/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link LastGroupProtocol} represents a protocol that adds group header to any message being sent.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class LastGroupProtocol extends AbstractProtocol
{
    private final UUID groupId;
    
    public LastGroupProtocol(String channelName, IMessageFactory messageFactory, UUID groupId)
    {
        super(channelName, null, messageFactory);
        
        Assert.notNull(groupId);
        
        this.groupId = groupId;
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        message = message.addPart(new GroupMessagePart(groupId));
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        message = message.addPart(new GroupMessagePart(groupId));
        return super.doSend(feed, sink, message);
    }
}