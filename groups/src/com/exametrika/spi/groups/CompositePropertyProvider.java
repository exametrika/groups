/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Assert;

/**
 * The {@link CompositePropertyProvider} is implementation of {@link IPropertyProvider}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositePropertyProvider implements IPropertyProvider
{
    private final List<IPropertyProvider> providers;
    
    public CompositePropertyProvider(List<IPropertyProvider> providers)
    {
        Assert.notNull(providers);
        
        this.providers = providers;
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (IPropertyProvider provider : providers)
            map.putAll(provider.getProperties());
        
        return map;
    }
}
