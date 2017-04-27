/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.channel;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.utils.Immutables;

/**
 * The {@link SystemPropertyProvider} is implementation of {@link IPropertyProvider}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SystemPropertyProvider implements IPropertyProvider
{
    @Override
    public Map<String, Object> getProperties()
    {
        Map<String, Object> map = new HashMap<String, Object>((Map)System.getProperties());
        map.putAll(System.getenv());
        return Immutables.wrap(map);
    }
}
