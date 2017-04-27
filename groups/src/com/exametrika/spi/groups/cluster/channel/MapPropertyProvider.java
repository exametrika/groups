/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.channel;

import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link MapPropertyProvider} is implementation of {@link IPropertyProvider}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MapPropertyProvider implements IPropertyProvider
{
    private final Map<String, Object> properties;
    
    public MapPropertyProvider(Map<String, Object> properties)
    {
        Assert.notNull(properties);
        
        this.properties = Immutables.wrap(properties);
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }
}
