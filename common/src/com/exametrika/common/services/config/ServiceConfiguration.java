/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.config;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link ServiceConfiguration} is a service configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ServiceConfiguration extends Configuration
{
    private final String name;
    private final String description;
    private final Set<ServiceProviderConfiguration> providers;

    public ServiceConfiguration(String name, String description, Set<ServiceProviderConfiguration> providers)
    {
        Assert.notNull(name);
        Assert.notNull(providers);
        
        this.name = name;
        this.description = description != null ? description : "";
        this.providers = Immutables.wrap(providers);
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Set<ServiceProviderConfiguration> getProviders()
    {
        return providers;
    }

    public List<ServiceProviderConfiguration> findProviders(Set<String> runModes, Set<String> qualifiers, boolean strict)
    {
        List<ServiceProviderConfiguration> list = new ArrayList<ServiceProviderConfiguration>();
        for (ServiceProviderConfiguration provider : providers)
        {
            if (provider.match(runModes, qualifiers, strict))
                list.add(provider);
        }
        return list;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof ServiceConfiguration))
            return false;
        
        ServiceConfiguration configuration = (ServiceConfiguration)o;
        return name.equals(configuration.name) && description.equals(configuration.description) && 
            providers.equals(configuration.providers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, description, providers);
    }
    
    @Override
    public String toString()
    {
        return MessageFormat.format("{0} ({1}):\n{2}", name, description, Strings.toString(providers, true));
    }
}
