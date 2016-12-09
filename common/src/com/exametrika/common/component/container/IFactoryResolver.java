/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.Map;

/**
 * The {@link IFactoryResolver} is used to resolve component factories outside a component container.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFactoryResolver
{
    /**
     * Resolves a component factory.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @param qualifiers required component qualifiers. Can be null
     * @return component factory or null if component factory is not found
     */
    <T> IComponentFactory<T> resolveFactory(String componentName, Map<String, ?> qualifiers); 
}
