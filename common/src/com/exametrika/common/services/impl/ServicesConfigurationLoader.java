/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.json.schema.JsonMetaSchemaFactory;
import com.exametrika.common.json.schema.JsonSchema;
import com.exametrika.common.json.schema.JsonSchemaLoader;
import com.exametrika.common.services.config.ServiceConfiguration;
import com.exametrika.common.services.config.ServiceProviderConfiguration;
import com.exametrika.common.services.config.ServicesConfiguration;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.IOs;

/**
 * The {@link ServicesConfigurationLoader} is a loader of services configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ServicesConfigurationLoader
{
    private final JsonSchema servicesSchema;
    
    public ServicesConfigurationLoader()
    {
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(Collections.<String>emptySet());
        JsonSchemaLoader loader = new JsonSchemaLoader();

        Reader reader = null;
        try
        {
            reader = new InputStreamReader(Classes.getResource(ServicesConfiguration.class, "services.schema"), "UTF-8");
            JsonObject object = JsonSerializers.read(reader, false);
            metaSchema.validate(object, "schema");
            servicesSchema = loader.loadSchema(object);
        }
        catch (IOException e)
        {
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
        finally
        {
            IOs.close(reader);
        }
    }
    
    public ServicesConfiguration load(ClassLoader classLoader)
    {
        Map<String, ServiceBuilder> servicesMap = new LinkedHashMap<String, ServiceBuilder>();
        
        try
        {
            String name = "META-INF/services.json";
            Enumeration<URL> resources;
            if (classLoader != null)
                resources = classLoader.getResources(name);
            else
                resources = ClassLoader.getSystemResources(name);
            
            while (resources.hasMoreElements())
            {
                URL resource = resources.nextElement();
                InputStream stream = resource.openStream();
        
                Reader reader = null;
                
                try
                {
                    reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    JsonObject servicesObject = JsonSerializers.read(reader, true);
                    servicesSchema.validate(servicesObject, "Services");
                    servicesObject = ((JsonObjectBuilder)servicesObject).toJson();
                    
                    for (Map.Entry<String, Object> serviceEntry : servicesObject)
                    {
                        ServiceBuilder builder = servicesMap.get(serviceEntry.getKey());
                        if (builder == null)
                        {
                            builder = new ServiceBuilder();
                            builder.name = serviceEntry.getKey();
                            builder.description = ((JsonObject)serviceEntry.getValue()).get("description", null);
                            servicesMap.put(serviceEntry.getKey(), builder);
                        }
                        
                        JsonObject providersObject = ((JsonObject)serviceEntry.getValue()).get("providers");
                        for (Map.Entry<String, Object> providerEntry : providersObject)
                        {
                            JsonObject provider = (JsonObject)providerEntry.getValue();
                            
                            String descripton = provider.get("description", null);
                            Set<String> runModes = (Set)JsonUtils.toSet((JsonArray)provider.get("runModes", null));
                            boolean runModeRequired = provider.get("runModeRequired");
                            Set<String> qualifiers = (Set)JsonUtils.toSet((JsonArray)provider.get("qualifiers", null));
                            boolean qualifiersRequired = provider.get("qualifiersRequired");
                            String className = provider.get("class");
                            
                            builder.providers.add(new ServiceProviderConfiguration(providerEntry.getKey(), descripton, 
                                className, runModes, runModeRequired, qualifiers, qualifiersRequired,  provider));
                        }
                    }
                }
                finally
                {
                    IOs.close(reader);
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
        
        Set<ServiceConfiguration> services = new LinkedHashSet<ServiceConfiguration>();
        for (ServiceBuilder builder : servicesMap.values())
            services.add(builder.toConfiguration());
        
        return new ServicesConfiguration(services);
    }
    
    private static class ServiceBuilder
    {
        private String name;
        private String description;
        private Set<ServiceProviderConfiguration> providers = new LinkedHashSet<ServiceProviderConfiguration>();
        
        public ServiceConfiguration toConfiguration()
        {
            return new ServiceConfiguration(name, description, providers);
        }
    }
}
