/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.Map;

/**
 * The {@link IComponentContainer} is a component container.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IComponentContainer
{
    /**
     * Registers a new component factory for specified name.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @param factory component factory
     */
    <T> void register(String componentName, IComponentFactory<T> factory);
    
    /**
     * Registers a new component factory for specified name and qualifiers.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @param qualifiers component qualifiers. Can be null
     * @param factory component factory
     */
    <T> void register(String componentName, Map<String, ?> qualifiers,  IComponentFactory<T> factory);
    
    /**
     * Unregisters component factory with specified name.
     *
     * @param componentName component name
     */
    void unregister(String componentName);
    
    /**
     * Unregisters component factory with specified name and qualifiers.
     *
     * @param componentName component name
     * @param qualifiers component qualifiers. Can be null
     */
    void unregister(String componentName, Map<String, ?> qualifiers);
    
    /**
     * Returns component instance for specified name from container.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @return component instance
     * @exception FactoryNotFoundException if component is not found in component container
     */
    <T> T getComponent(String componentName);
    
    /**
     * Returns component instance for specified name and qualifiers from container.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @param qualifiers required component qualifiers. Can be null
     * @param required if true component is required, else component is optional
     * @return component instance or null if optional component is not found in component container
     * @exception FactoryNotFoundException if required component is not found in component container
     */
    <T> T getComponent(String componentName, Map<String, ?> qualifiers, boolean required);
    
    /**
     * Returns component factory for specified name.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @return component factory
     * @exception FactoryNotFoundException if component factory is not found in component container
     */
    <T> IComponentFactory<T> getFactory(String componentName);
    
    /**
     * Returns component factory for specified name and qualifiers from container.
     *
     * @param <T> type name of component
     * @param componentName component name
     * @param qualifiers required component qualifiers. Can be null
     * @param required if true component is required, else component is optional
     * @return component factory or null if optional component factory is not found in component container
     * @exception FactoryNotFoundException if required component factory is not found in component container
     */
    <T> IComponentFactory<T> getFactory(String componentName, Map<String, ?> qualifiers, boolean required);
}
