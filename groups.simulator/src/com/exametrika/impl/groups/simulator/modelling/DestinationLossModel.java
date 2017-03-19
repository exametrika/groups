/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DestinationLossModel} is a message loss model that drops messages sent to specified destinations.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DestinationLossModel implements ILossModel
{
    private final Set<IAddress> destinations;

    public DestinationLossModel(Set<IAddress> destinations)
    {
        Assert.notNull(destinations);
        
        this.destinations = destinations;
    }

    @Override
    public boolean canDropMessage(IMessage message)
    {
        return destinations.contains(message.getDestination());
    }
}
