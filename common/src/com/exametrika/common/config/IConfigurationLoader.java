/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.config.property.IPropertyResolver;
import com.exametrika.common.config.resource.IResourceLoader;
import com.exametrika.common.json.JsonMacroses.MacroDefinition;
import com.exametrika.common.json.schema.IJsonConverter;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.utils.Pair;



/**
 * The {@link IConfigurationLoader} is used to load configuration.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConfigurationLoader
{
    /**
     * The {@link Parameters} represents parameters of {@link IConfigurationLoader}.
     * 
     * @author Medvedev-A
     */
    public class Parameters
    {
        /**
         * Map of custom top-level element loaders with key {qualified element name}.
         */
        public final Map<String, IElementLoader> elementLoaders = new LinkedHashMap<String, IElementLoader>();
        
        /**
         * Map of custom type-based element loaders with key {qualified element type}.
         */
        public final Map<String, IExtensionLoader> typeLoaders = new LinkedHashMap<String, IExtensionLoader>();
        
        /**
         * List of context factories with key {configuration type}.
         */
        public final Map<String, IContextFactory> contextFactories = new LinkedHashMap<String, IContextFactory>();
        
        /**
         * List of configuration parsers with key {format type}. Default format type is empty string. 
         * Default format type is used when format type of configuration can not be safely recognized.
         */
        public final Map<String, IConfigurationParser> configurationParsers = new LinkedHashMap<String, IConfigurationParser>();
        
        /**
         * List of custom resource loaders with key {schemaName}.
         */
        public final Map<String, IResourceLoader> resourceLoaders = new LinkedHashMap<String, IResourceLoader>();
        
        /**
         * List of schema name to schema resource mappings with key {schemaName} and value {schemaResourceName:lazyLoad}.
         */
        public final Map<String, Pair<String, Boolean>> schemaMappings = new LinkedHashMap<String, Pair<String, Boolean>>();
        
        /**
         * List of top level configuration elements with key {elementName} and value {typeName:required}.
         */
        public final Map<String, Pair<String, Boolean>> topLevelElements = new LinkedHashMap<String, Pair<String, Boolean>>();
        
        /**
         * List of custom property resolvers.
         */
        public final List<IPropertyResolver> propertyResolvers = new ArrayList<IPropertyResolver>();
        
        /**
         * List of custom schema validators with key {validatorName}.
         */
        public final Map<String, IJsonValidator> validators = new LinkedHashMap<String, IJsonValidator>();
        
        /**
         * List of custom schema value converters with key {converterName}.
         */
        public final Map<String, IJsonConverter> converters = new LinkedHashMap<String, IJsonConverter>();
        
        /**
         * List of system configurations.
         */
        public final List<String> systemConfigurations = new ArrayList<String>();
        
        /**
         * Predefined global macroses - static elements or functions.
         */
        public final List<MacroDefinition> macroses = new ArrayList<MacroDefinition>();

        /**
         * Combines this configuration parameters with specified configuration parameters.
         *
         * @param parameters configuration parameters to combine
         */
        public void combine(Parameters parameters)
        {
            elementLoaders.putAll(parameters.elementLoaders);
            typeLoaders.putAll(parameters.typeLoaders);
            contextFactories.putAll(parameters.contextFactories);
            configurationParsers.putAll(parameters.configurationParsers);
            resourceLoaders.putAll(parameters.resourceLoaders);
            schemaMappings.putAll(parameters.schemaMappings);
            propertyResolvers.addAll(parameters.propertyResolvers);
            validators.putAll(parameters.validators);
            converters.putAll(parameters.converters);
            topLevelElements.putAll(parameters.topLevelElements);
            systemConfigurations.addAll(parameters.systemConfigurations);
            macroses.addAll(parameters.macroses);
        }
    }

    /**
     * Loads configuration.
     *
     * @param configurationLocation location of configuration file
     * @return configuration load context
     * @exception InvalidConfigurationException if configuration is not valid
     */
    ILoadContext loadConfiguration(String configurationLocation);
}
