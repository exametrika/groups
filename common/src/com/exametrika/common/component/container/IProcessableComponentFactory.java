/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.List;

/**
 * The {@link IProcessableComponentFactory} is a component factory that can process component types.
 * 
 * @param <T> type name of component
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IProcessableComponentFactory<T> extends IComponentFactory<T>
{
    /**
     * Sets type processors.
     *
     * @param typeProcessors type processors
     */
    void setTypeProcessors(List<? extends ITypeProcessor> typeProcessors);
    
    /**
     * Sets component processors.
     *
     * @param componentProcessors component processors
     */
    void setComponentProcessors(List<? extends IComponentProcessor> componentProcessors);
}
