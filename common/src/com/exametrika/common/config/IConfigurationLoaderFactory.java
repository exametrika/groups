/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import java.util.Map;




/**
 * The {@link IConfigurationLoaderFactory} is factory for configuration loader.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConfigurationLoaderFactory
{
    /**
     * Creates configuration loader.
     *
     * @param initParameters init parameters
     * @return configuration loader
     */
    IConfigurationLoader createLoader(Map<String, Object> initParameters);
}
