/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.channel;

import java.util.Map;


/**
 * The {@link IPropertyProvider} represents a provider of properties.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IPropertyProvider
{
    /**
     * Returns properties.
     *
     * @return properties
     */
    Map<String, Object> getProperties();
}