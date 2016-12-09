/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link ServicesConfiguration} is a services configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ServicesConfiguration extends Configuration
{
    private final Set<ServiceConfiguration> services;
    private final Map<String, ServiceConfiguration> servicesMap;

    public ServicesConfiguration(Set<ServiceConfiguration> services)
    {
        Assert.notNull(services);
        
        Map<String, ServiceConfiguration> servicesMap = new HashMap<String, ServiceConfiguration>();
        for (ServiceConfiguration service : services)
            Assert.isNull(servicesMap.put(service.getName(), service));

        this.services = Immutables.wrap(services);
        this.servicesMap = servicesMap;
    }

    public Set<ServiceConfiguration> getServices()
    {
        return services;
    }

    public ServiceConfiguration findService(String name)
    {
        Assert.notNull(name);
        
        return servicesMap.get(name);
    }
    
    public List<ServiceProviderConfiguration> findProviders(String name, Set<String> runModes, Set<String> qualifiers, boolean strict)
    {
        Assert.notNull(name);
        
        ServiceConfiguration service = servicesMap.get(name);
        if (service == null)
            return Collections.emptyList();
        else
            return service.findProviders(runModes, qualifiers, strict);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof ServicesConfiguration))
            return false;
        
        ServicesConfiguration configuration = (ServicesConfiguration)o;
        return services.equals(configuration.services);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(services);
    }
    
    @Override
    public String toString()
    {
        return Strings.toString(services, false);
    }
}
