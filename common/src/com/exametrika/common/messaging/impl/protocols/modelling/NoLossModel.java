/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.modelling;

import com.exametrika.common.messaging.IMessage;

/**
 * The {@link NoLossModel} is a message loss model that never drops messages.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NoLossModel implements ILossModel
{
    @Override
    public boolean canDropMessage(IMessage message)
    {
        return false;
    }
}
