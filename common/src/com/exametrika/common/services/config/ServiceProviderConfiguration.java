/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.config;

import java.lang.reflect.Constructor;
import java.util.ServiceConfigurationError;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ServiceProviderConfiguration} is a service provider configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ServiceProviderConfiguration extends Configuration
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final String description;
    private final String className;
    private final Set<String> runModes;
    private final boolean runModeRequired;
    private final Set<String> qualifiers;
    private final boolean qualifiersRequired;
    private final JsonObject parameters;

    public ServiceProviderConfiguration(String name, String description, String className, Set<String> runModes, 
        boolean runModeRequired, Set<String> qualifiers, boolean qualifiersRequired, JsonObject parameters)
    {
        Assert.notNull(name);
        Assert.notNull(className);
        Assert.notNull(parameters);
        
        this.name = name;
        this.description = description != null ? description : "";
        this.className = className;
        this.runModes = Immutables.wrap(runModes);
        this.runModeRequired = runModeRequired;
        this.qualifiers = Immutables.wrap(qualifiers);
        this.qualifiersRequired = qualifiersRequired;
        this.parameters = parameters;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getClassName()
    {
        return className;
    }

    public Set<String> getRunModes()
    {
        return runModes;
    }

    public boolean isRunModeRequired()
    {
        return runModeRequired;
    }
    
    public Set<String> getQualifiers()
    {
        return qualifiers;
    }

    public boolean getQualifiersRequired()
    {
        return qualifiersRequired;
    }
    
    public JsonObject getParameters()
    {
        return parameters;
    }
    
    public boolean match(Set<String> runModes, Set<String> qualifiers, boolean strict)
    {
        if (!match(runModes, this.runModes, runModeRequired, strict))
            return false;
        
        return match(qualifiers, this.qualifiers, qualifiersRequired, true);
    }

    public <T> T createInstance(Class<T> serviceClass, ClassLoader classLoader, Set<String> runModes)
    {
        Assert.notNull(serviceClass);
        if (className.equals("configuration"))
            throw new UnsupportedOperationException(messages.instantiationError(name, serviceClass.getName()).toString());
        
        try
        {
            Class<?> providerClass = Class.forName(className, true, classLoader);
            Constructor<?>[] constructors = providerClass.getConstructors();
            Constructor<?> providerConstructor = null;
            for (int i = 0; i < constructors.length; i++)
            {
                Constructor<?> constructor = constructors[i];
                Class<?>[] parameterTypes = constructor.getParameterTypes(); 
                if (parameterTypes.length == 3 && parameterTypes[0] == String.class &&
                    parameterTypes[1] == Set.class && parameterTypes[2] == JsonObject.class)
                {
                    providerConstructor = constructor;
                    break;
                }
            }
            
            if (providerConstructor != null)
                return serviceClass.cast(providerConstructor.newInstance(name, runModes, parameters));
            else
                return serviceClass.cast(providerClass.newInstance());
        }
        catch (Exception e)
        {
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof ServiceProviderConfiguration))
            return false;
        
        ServiceProviderConfiguration configuration = (ServiceProviderConfiguration)o;
        return name.equals(configuration.name) && description.equals(configuration.description) && 
            className.equals(configuration.className) && Objects.equals(runModes, configuration.runModes) &&
            runModeRequired == configuration.runModeRequired && Objects.equals(qualifiers, configuration.qualifiers) && 
            qualifiersRequired == configuration.qualifiersRequired && parameters.equals(configuration.parameters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, description, className, runModes, runModeRequired, qualifiers, qualifiers, parameters);
    }
    
    @Override
    public String toString()
    {
        return parameters.toString();
    }
    
    private static boolean match(Set<String> qualifiers, Set<String> serviceQualifiers, boolean qualifierRequired, boolean strict)
    {
        if (qualifiers != null && !qualifiers.isEmpty())
        {
            if (serviceQualifiers != null)
            {
                boolean found = false;
                for (String runMode : serviceQualifiers)
                {
                    String[] parts = runMode.split("&");
                    boolean partFound = true;
                    for (String part : parts)
                    {
                        part = part.trim();
                        if (part.isEmpty())
                            continue;
                        
                        if (!qualifiers.contains(part))
                        {
                            partFound = false;
                            break;
                        }
                    }
                    
                    if (partFound)
                    {
                        found = true;
                        break;
                    }
                }
                
                if (!found)
                    return false;
            }
            else if (strict)
                return false;
        }
        else if (qualifierRequired)
            return false;
        
        return true;
    }

    private interface IMessages
    {
        @DefaultMessage("Could not create an instance of configuration only provider ''{0}'' of service ''{1}''.")
        ILocalizedMessage instantiationError(String providerName, String serviceName);
    }
}
