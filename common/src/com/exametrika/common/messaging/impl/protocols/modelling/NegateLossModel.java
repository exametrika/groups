/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.modelling;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;

/**
 * The {@link NegateLossModel} is a message loss model that negates base loss model.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NegateLossModel implements ILossModel
{
    private final ILossModel model;

    /**
     * Creates a new object.
     *
     * @param model base model
     */
    public NegateLossModel(ILossModel model)
    {
        Assert.notNull(model);
        
        this.model = model;
    }

    @Override
    public boolean canDropMessage(IMessage message)
    {
        return !model.canDropMessage(message);
    }
}
