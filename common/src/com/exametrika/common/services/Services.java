/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.services.config.ServiceConfiguration;
import com.exametrika.common.services.config.ServiceProviderConfiguration;
import com.exametrika.common.services.config.ServicesConfiguration;
import com.exametrika.common.services.impl.ServicesConfigurationLoader;
import com.exametrika.common.utils.Assert;

/**
 * The {@link Services} contains different utility methods for work with services.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Services
{
    private static final boolean DEBUG = System.getenv("EXA_DEBUG") != null || System.getProperty("com.exametrika.debug", "false").equals("true");
    private static volatile Set<String> defaultRunModes;
    private static final Map<ClassLoader, ServicesConfiguration> services = new HashMap<ClassLoader, ServicesConfiguration>();

    public static Set<String> getDefaultRunModes()
    {
        return defaultRunModes;
    }
    
    public static void setDefaultRunModes(Set<String> runModes)
    {
        defaultRunModes = runModes;
    }
    
    /**
     * Loads service provider.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @return service provider or null if service provider is not available
     */
    public static <T> T loadProvider(Class<T> serviceClass)
    {
        Assert.notNull(serviceClass);
        return loadProvider(serviceClass, serviceClass.getClassLoader());
    }
    
    /**
     * Loads service provider.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @param classLoader class loader to load services from
     * @return service provider or null if service provider is not available
     */
    public static <T> T loadProvider(Class<T> serviceClass, ClassLoader classLoader)
    {
        List<T> providers = loadProviders(serviceClass, defaultRunModes, null, classLoader, false);
        if (providers.size() > 0)
            return providers.get(0);
        else
            return null;
    }
    
    /**
     * Loads service providers.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @return list of service providers
     */
    public static <T> List<T> loadProviders(Class<T> serviceClass)
    {
        Assert.notNull(serviceClass);
        return loadProviders(serviceClass, defaultRunModes, null, serviceClass.getClassLoader(), false);
    }
    
    /**
     * Loads service providers.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @param classLoader class loader to load services from
     * @return list of service providers
     */
    public static <T> List<T> loadProviders(Class<T> serviceClass, ClassLoader classLoader)
    {
        Assert.notNull(serviceClass);
        return loadProviders(serviceClass, defaultRunModes, null, classLoader, false);
    }
    
    /**
     * Loads service providers.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @param qualifiers set of qualifiers services provider must match to. If qualifiers are set, creates services 
     *      supported one of specified qualifiers
     * @return list of service providers
     */
    public static <T> List<T> loadProviders(Class<T> serviceClass, Set<String> qualifiers)
    {
        Assert.notNull(serviceClass);
        return loadProviders(serviceClass, defaultRunModes, qualifiers, serviceClass.getClassLoader(), false);
    }
    
    /**
     * Loads service providers.
     *
     * @param <T> serviceType
     * @param serviceClass class of service
     * @param runModes run modes. If run modes are set, creates services supporting specified run modes only
     * @param qualifiers set of qualifiers services provider must match to. If qualifiers are set, creates services 
     *        supported one of specified qualifiers
     * @param classLoader class loader to load services from
     * @param strict if true and runModes are set, all service providers with undefined runModes are ignored
     * @return list of service providers
     */
    public static <T> List<T> loadProviders(Class<T> serviceClass, Set<String> runModes, Set<String> qualifiers, ClassLoader classLoader,
        boolean strict)
    {
        Assert.notNull(serviceClass);
        
        ServicesConfiguration configuration = loadConfiguration(classLoader);
        List<ServiceProviderConfiguration> providerConfigurations = configuration.findProviders(serviceClass.getName(), 
            runModes, qualifiers, strict);
        List<T> providers = new ArrayList<T>(providerConfigurations.size());
        for (ServiceProviderConfiguration providerConfiguration : providerConfigurations)
            providers.add(providerConfiguration.createInstance(serviceClass, classLoader, runModes));
        
        return providers;
    }
    
    /**
     * Loads service configuration.
     *
     * @param serviceClass class of service
     * @param classLoader class loader to load services from
     * @return service configuration of null if service is not available
     */
    public static ServiceConfiguration loadServiceConfiguration(Class<?> serviceClass, ClassLoader classLoader)
    {
        Assert.notNull(serviceClass);
        
        ServicesConfiguration configuration = loadConfiguration(classLoader);
        return configuration.findService(serviceClass.getName());
    }
    
    /**
     * Loads service configuration.
     *
     * @param serviceClass class of service
     * @return service configuration of null if service is not available
     */
    public static ServiceConfiguration loadServiceConfiguration(Class<?> serviceClass)
    {
        return loadServiceConfiguration(serviceClass, serviceClass.getClassLoader());
    }

    /**
     * Loads service provider configurations.
     *
     * @param serviceClass class of service
     * @return list of service provider configurations
     */
    public static List<ServiceProviderConfiguration> loadProviderConfigurations(Class<?> serviceClass)
    {
        Assert.notNull(serviceClass);
        return loadProviderConfigurations(serviceClass, defaultRunModes, null, serviceClass.getClassLoader(), false);
    }
    
    /**
     * Loads service provider configurations.
     *
     * @param serviceClass class of service
     * @param classLoader class loader to load services from
     * @return list of service providers
     */
    public static List<ServiceProviderConfiguration> loadProviderConfigurations(Class<?> serviceClass, ClassLoader classLoader)
    {
        Assert.notNull(serviceClass);
        return loadProviderConfigurations(serviceClass, defaultRunModes, null, classLoader, false);
    }
    
    /**
     * Loads service provider configurations.
     *
     * @param serviceClass class of service
     * @param qualifiers set of qualifiers services provider must match to. If qualifiers are set, creates services 
     *      supported one of specified qualifiers
     * @return list of service provider configurations
     */
    public static List<ServiceProviderConfiguration> loadProviderConfigurations(Class<?> serviceClass, Set<String> qualifiers)
    {
        Assert.notNull(serviceClass);
        return loadProviderConfigurations(serviceClass, defaultRunModes, qualifiers, serviceClass.getClassLoader(), false);
    }
    
    /**
     * Loads service provider configurations.
     *
     * @param serviceClass class of service
     * @param runModes run modes. If run modes are set, creates services supporting specified run modes only
     * @param qualifiers set of qualifiers services provider must match to. If qualifiers are set, creates services 
     *        supported one of specified qualifiers
     * @param classLoader class loader to load services from
     * @param strict if true and runModes are set, all service providers with undefined runModes are ignored
     * @return list of service provider configurations
     */
    public static List<ServiceProviderConfiguration> loadProviderConfigurations(Class<?> serviceClass, Set<String> runModes, 
        Set<String> qualifiers, ClassLoader classLoader, boolean strict)
    {
        Assert.notNull(serviceClass);
        
        ServicesConfiguration configuration = loadConfiguration(classLoader);
        return configuration.findProviders(serviceClass.getName(), runModes, qualifiers, strict);
    }

    private static synchronized ServicesConfiguration loadConfiguration(ClassLoader classLoader)
    {
        ServicesConfiguration configuration = services.get(classLoader);
        if (configuration != null)
            return configuration;
        
        ServicesConfigurationLoader loader = new ServicesConfigurationLoader();
        configuration = loader.load(classLoader);
        services.put(classLoader, configuration);
        
        if (DEBUG)
            System.out.println(configuration.toString());
        
        return configuration;
    }
    
    private Services()
    {
    }
}
