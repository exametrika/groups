/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;


/**
 * The {@link IExtensionLoader} is used to load given type-based extension of configuration.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IExtensionLoader
{
    /**
     * Loads configuration element.
     *
     * @param name name of element or null if name is not available
     * @param type type of element or null if type is not available
     * @param element configuration element
     * @param context load context
     * @return extension configuration
     */
    Object loadExtension(String name, String type, Object element, ILoadContext context);
    
    /**
     * Sets extension loader.
     *
     * @param extensionLoader extension loader
     */
    void setExtensionLoader(IExtensionLoader extensionLoader);
}
