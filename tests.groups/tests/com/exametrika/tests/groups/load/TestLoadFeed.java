/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.utils.Assert;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;

public final class TestLoadFeed implements IFeed
{
    private final TestLoadMessageSender sender;
    
    public TestLoadFeed(TestLoadMessageSender sender)
    {
        Assert.notNull(sender);
        
        this.sender = sender;
    }
    
    @Override
    public void feed(ISink sink)
    {
        if (sender.allowSend(SendType.PULLABLE))
        {
            IMessage message = sink.getMessageFactory().create(sink.getDestination(), sender.createPart());
            sink.send(message);
        }
    }
}