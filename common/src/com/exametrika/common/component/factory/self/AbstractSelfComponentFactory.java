/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.self;

import com.exametrika.common.component.factory.AbstractComponentFactory;

/**
 * The {@link AbstractSelfComponentFactory} is an abstract component factory which is a component itself.
 *
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractSelfComponentFactory<T> extends AbstractComponentFactory<T>
{
    @Override
    public final T createComponent()
    {
        // Factory is a component itself.
        return (T)this;
    }
}
