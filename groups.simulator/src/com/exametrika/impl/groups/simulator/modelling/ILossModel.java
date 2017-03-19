/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import com.exametrika.common.messaging.IMessage;

/**
 * The {@link ILossModel} represents a message loss model.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILossModel
{
    /**
     * Can the specified message be dropped?
     *
     * @param message message to drop
     * @return true if message can be dropped, false if message can not be dropped
     */
    boolean canDropMessage(IMessage message);
}
