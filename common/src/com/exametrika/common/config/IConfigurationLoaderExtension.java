/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import com.exametrika.common.config.IConfigurationLoader.Parameters;



/**
 * The {@link IConfigurationLoaderExtension} is used to extend process of configuration loading.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConfigurationLoaderExtension
{
    /**
     * Returns parameters of configuration loader.
     *
     * @return parameters of configuration loader
     */
    Parameters getParameters();
}
