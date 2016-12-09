/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.IFlowController;


/**
 * The {@link NoFlowController} is a {@link IFlowController} implementation that
 * does not control any flow.
 *
 * @param <T> flow type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class NoFlowController<T> implements IFlowController<T>
{
    @Override
    public void lockFlow(T flow)
    {
    }

    @Override
    public void unlockFlow(T flow)
    {
    }
}
