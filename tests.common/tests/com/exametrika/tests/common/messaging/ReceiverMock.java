/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ReceiverMock} is a mock implementation of {@link IReceiver}.
 * 
 * @author medvedev
 */
public class ReceiverMock implements IReceiver
{
    public List<IMessage> messages = new ArrayList<IMessage>();
    public int index;
    public final int count;
    private Sequencer sequencer;
    
    public ReceiverMock()
    {
        count = 0;
        sequencer = null;
    }
    
    public ReceiverMock(int count, Sequencer sequencer)
    {
        this.count = count;
        this.sequencer = sequencer;
    }
    
    @Override
    public synchronized void receive(IMessage message)
    {
        Assert.notNull(message);
        messages.add(message);
        
        index++;
        if (index == count && sequencer != null)
            sequencer.allowSingle("Received " + message.getSource());
    }
}