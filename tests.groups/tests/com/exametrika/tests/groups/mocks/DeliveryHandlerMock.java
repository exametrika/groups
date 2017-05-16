/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;

public class DeliveryHandlerMock implements IDeliveryHandler
{
    public List<IMessage> messages = new ArrayList<IMessage>();
    
    @Override
    public void onDelivered(IMessage message)
    {
        messages.add(message);
    }
}
