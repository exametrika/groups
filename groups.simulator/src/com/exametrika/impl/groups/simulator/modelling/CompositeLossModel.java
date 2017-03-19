/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.List;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link CompositeLossModel} is a composition of message loss models.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositeLossModel implements ILossModel, ILifecycle
{
    private final List<ILossModel> models;
    private volatile boolean started;

    /**
     * Creates a new object.
     *
     * @param models composing models
     */
    public CompositeLossModel(List<ILossModel> models)
    {
        Assert.notNull(models);
        
        this.models = models;
    }

    @Override
    public boolean canDropMessage(IMessage message)
    {
        if (!started)
            return false;
        
        for (ILossModel model : models)
        {
            if (model.canDropMessage(message))
                return true;
        }
        
        return false;
    }

    @Override
    public void start()
    {
        started = true;
    }

    @Override
    public void stop()
    {
        started = false;
    }
}
