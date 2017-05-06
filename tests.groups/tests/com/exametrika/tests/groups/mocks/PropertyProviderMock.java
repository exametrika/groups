/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.Map;

import com.exametrika.common.utils.MapBuilder;
import com.exametrika.spi.groups.cluster.channel.IPropertyProvider;

public class PropertyProviderMock implements IPropertyProvider
{
    public Map<String, Object> properties = new MapBuilder<String, Object>().put("key", "value").toMap();
    
    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }
}