/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;

import java.util.Map;

import com.exametrika.common.utils.Assert;



/**
 * The {@link MapPropertyResolver} is an implementation of {@link IPropertyResolver} that 
 * uses specified map to resolve properties.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MapPropertyResolver implements IPropertyResolver
{
    private final Map<String, String> properties;

    public MapPropertyResolver(Map<String, String> properties)
    {
        Assert.notNull(properties);
        
        this.properties = properties;
    }
    
    @Override
    public String resolveProperty(String propertyName)
    {
        Assert.notNull(propertyName);
        
        return properties.get(propertyName);
    }
}
