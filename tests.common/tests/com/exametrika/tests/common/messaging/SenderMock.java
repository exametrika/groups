/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;

/**
 * The {@link SenderMock} is a mock implementation of {@link ISender}.
 * 
 * @author medvedev
 */
public class SenderMock implements ISender
{
    public List<IMessage> messages = new ArrayList<IMessage>();
    
    @Override
    public synchronized void send(IMessage message)
    {
        Assert.notNull(message);
        messages.add(message);
    }
}