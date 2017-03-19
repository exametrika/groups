/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Set;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;

/**
 * The {@link SourceLossModel} is a message loss model that drops messages received from specified sources.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SourceLossModel implements ILossModel
{
    private final Set<IAddress> sources;

    public SourceLossModel(Set<IAddress> sources)
    {
        Assert.notNull(sources);
        
        this.sources = sources;
    }

    @Override
    public boolean canDropMessage(IMessage message)
    {
        return sources.contains(message.getSource());
    }
}
