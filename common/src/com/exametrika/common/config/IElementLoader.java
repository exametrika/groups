/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.json.JsonObject;

/**
 * The {@link IElementLoader} is used to load given element of configuration.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IElementLoader
{
    /**
     * Loads configuration element into specified context.
     *
     * @param element configuration element
     * @param context load context
     */
    void loadElement(JsonObject element, ILoadContext context);
    
    /**
     * Sets extension loader.
     *
     * @param extensionLoader extension loader
     */
    void setExtensionLoader(IExtensionLoader extensionLoader);
}
