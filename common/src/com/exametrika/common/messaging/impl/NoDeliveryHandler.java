/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;

/**
 * The {@link NoDeliveryHandler} represents a no deluvery handler.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NoDeliveryHandler implements IDeliveryHandler
{
    @Override
    public void onDelivered(IMessage message)
    {
    }
}
