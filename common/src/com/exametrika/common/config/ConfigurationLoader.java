/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.common.CommonConfigurationExtention;
import com.exametrika.common.config.parsers.JsonConfigurationParser;
import com.exametrika.common.config.property.CompositePropertyResolver;
import com.exametrika.common.config.property.IPropertyResolver;
import com.exametrika.common.config.property.MapPropertyResolver;
import com.exametrika.common.config.property.Properties;
import com.exametrika.common.config.property.SystemPropertyResolver;
import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.config.resource.IResourceManager;
import com.exametrika.common.config.resource.InlineResourceLoader;
import com.exametrika.common.config.resource.ResourceManager;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonMacroses;
import com.exametrika.common.json.JsonMacroses.MacroDefinition;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.json.schema.IJsonConverter;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonMetaSchemaFactory;
import com.exametrika.common.json.schema.JsonSchema;
import com.exametrika.common.json.schema.JsonSchemaLoader;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ConfigurationLoader} is an implementation of {@link IConfigurationLoader}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ConfigurationLoader implements IConfigurationLoader, IExtensionLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<String, Object> initParameters;
    private final Map<String, IElementLoader> elementLoaders;
    private final Map<String, IExtensionLoader> typeLoaders;
    private final Map<String, IConfigurationParser> configurationParsers;
    private final IResourceManager resourceLoader;
    private final Map<String, Pair<String, Boolean>> schemaMappings;
    private final List<IPropertyResolver> propertyResolvers;
    private final Map<String, IContextFactory> contextFactories;
    private final Map<String, IJsonValidator> validators;
    private final Map<String, IJsonConverter> converters;
    private final Map<String, Pair<String, Boolean>> topLevelElements;
    private final List<String> systemConfigurations;
    private final List<MacroDefinition> macroses;

    public ConfigurationLoader()
    {
        this(new Parameters(), Collections.<String, Object>emptyMap(), null, true);
    }
    
    public ConfigurationLoader(Set<String> qualifiers)
    {
        this(new Parameters(), Collections.<String, Object>emptyMap(), qualifiers, true);
    }
    
    public ConfigurationLoader(Map<String, Object> initParameters)
    {
        this(new Parameters(), initParameters, null, true);
    }
    
    public ConfigurationLoader(Parameters parameters, Map<String, Object> initParameters, Set<String> qualifiers, 
        boolean createDefaultParameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(initParameters);
        
        if (createDefaultParameters)
        {
            Parameters defaultParameters = createDefaultParameters(qualifiers);
            parameters.combine(defaultParameters);
        }
        
        this.initParameters = initParameters;
        this.elementLoaders = parameters.elementLoaders;
        this.typeLoaders = parameters.typeLoaders;
        this.configurationParsers = parameters.configurationParsers;
        this.resourceLoader = new ResourceManager(parameters.resourceLoaders, "file");
        this.schemaMappings = parameters.schemaMappings;
        this.propertyResolvers = parameters.propertyResolvers;
        this.contextFactories = parameters.contextFactories;
        this.validators = parameters.validators;
        this.converters = parameters.converters;
        this.topLevelElements = parameters.topLevelElements;
        this.systemConfigurations = parameters.systemConfigurations;
        this.macroses = parameters.macroses;
        
        for (Map.Entry<String, IElementLoader> entry : elementLoaders.entrySet())
            entry.getValue().setExtensionLoader(this);
        
        for (Map.Entry<String, IExtensionLoader> entry : typeLoaders.entrySet())
            entry.getValue().setExtensionLoader(this);
    }
    
    @Override
    public ILoadContext loadConfiguration(String configurationLocation)
    {
        Assert.notNull(configurationLocation);
        
        if (!resourceLoader.hasSchema(configurationLocation))
            configurationLocation = "file:" + configurationLocation;
        
        Map<String, Object> contexts = new LinkedHashMap<String, Object>(contextFactories.size());
        for (Map.Entry<String, IContextFactory> entry : contextFactories.entrySet())
            contexts.put(entry.getKey(), entry.getValue().createContext());
        
        LoadContext loadContext = new LoadContext(contexts, initParameters);
        
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(validators.keySet());
        
        for (Map.Entry<String, Pair<String, Boolean>> entry : schemaMappings.entrySet())
        {
            String schemaLocation = entry.getValue().getKey();
            boolean lazyLoad = entry.getValue().getValue();
            if (!lazyLoad)
                loadConfiguration(schemaLocation, loadContext, true, null, true);
        }
        
        for (String systemConfiguration : systemConfigurations)
            loadConfiguration(systemConfiguration, loadContext, false, null, true);
        
        loadConfiguration(configurationLocation, loadContext, false, null, true);
        
        if (loadContext.rootElement == null)
            throw new InvalidConfigurationException(messages.rootElementNotFound(configurationLocation));
        if (loadContext.schemaElement == null)
            throw new InvalidConfigurationException(messages.schemaNotFound(configurationLocation));

        createRootElementType(loadContext);
        
        try
        {
            metaSchema.validate(loadContext.schemaElement, "schema");
            
            JsonSchemaLoader schemaLoader = new JsonSchemaLoader(validators, converters, true);
            JsonSchema schema = schemaLoader.loadSchema(loadContext.schemaElement);
            
            JsonMacroses.expandMacroses(loadContext.rootElement, "Configuration", macroses);
            schema.validate(loadContext.rootElement, "Configuration");
            loadRootElement(loadContext.rootElement.toJson(), loadContext);
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(messages.invalidConfiguration(configurationLocation), e);
        }
        
        return loadContext.createConfigurationContext();
    }

    @Override
    public Object loadExtension(String name, String type, Object element, ILoadContext context)
    {
        if (element instanceof JsonObject && ((JsonObject)element).contains("instanceOf"))
            type = ((JsonObject)element).get("instanceOf");
            
        if (type != null)
        {
            IExtensionLoader typeLoader = typeLoaders.get(type);
            if (typeLoader != null)
                return typeLoader.loadExtension(name, type, element, context);
            else
                throw new InvalidConfigurationException(messages.loaderNotFoundType(type));
        }
        else
            throw new InvalidConfigurationException(messages.typeNotSetForNestedElement(element));
    }

    @Override
    public void setExtensionLoader(IExtensionLoader extensionLoader)
    {
        Assert.supports(false);
    }
    
    public String createInlineConfiguration(String configurationLocation, IPropertyResolver propertyResolver)
    {
        Assert.notNull(configurationLocation);
        if (configurationLocation.startsWith("inline:"))
            return configurationLocation;
        
        if (!resourceLoader.hasSchema(configurationLocation))
            configurationLocation = "file:" + configurationLocation;
        
        Map<String, Object> contexts = new LinkedHashMap<String, Object>(contextFactories.size());
        for (Map.Entry<String, IContextFactory> entry : contextFactories.entrySet())
            contexts.put(entry.getKey(), entry.getValue().createContext());
        
        LoadContext loadContext = new LoadContext(contexts, initParameters);
        
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(validators.keySet());
        
        for (Map.Entry<String, Pair<String, Boolean>> entry : schemaMappings.entrySet())
        {
            String schemaLocation = entry.getValue().getKey();
            boolean lazyLoad = entry.getValue().getValue();
            if (!lazyLoad)
                loadConfiguration(schemaLocation, loadContext, true, null, true);
        }
        
        for (String systemConfiguration : systemConfigurations)
            loadConfiguration(systemConfiguration, loadContext, false, propertyResolver, false);
        
        loadConfiguration(configurationLocation, loadContext, false, propertyResolver, false);
        
        if (loadContext.rootElement == null)
            throw new InvalidConfigurationException(messages.rootElementNotFound(configurationLocation));
        if (loadContext.schemaElement == null)
            throw new InvalidConfigurationException(messages.schemaNotFound(configurationLocation));

        createRootElementType(loadContext);
        
        try
        {
            metaSchema.validate(loadContext.schemaElement, "schema");
            
            JsonSchemaLoader schemaLoader = new JsonSchemaLoader(validators, converters, true);
            JsonSchema schema = schemaLoader.loadSchema(loadContext.schemaElement);
            
            JsonMacroses.expandMacroses(loadContext.rootElement, "Configuration", macroses);
            schema.validate(loadContext.rootElement, "Configuration");
            
            return JsonSerializers.write(loadContext.rootElement, true);
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(messages.invalidConfiguration(configurationLocation), e);
        }
    }
    
    public ILoadContext loadInlineConfiguration(String configuration)
    {
        Assert.notNull(configuration);
        
        List<IPropertyResolver> propertyResolvers = new ArrayList<IPropertyResolver>(this.propertyResolvers);
        IPropertyResolver propertyResolver = new CompositePropertyResolver(propertyResolvers);
        
        String contents = Properties.expandProperties(new JsonConfigurationParser(), propertyResolver, configuration, true, true);
        
        JsonObject rootElement = JsonSerializers.read(contents, false);
        
        Map<String, Object> contexts = new LinkedHashMap<String, Object>(contextFactories.size());
        for (Map.Entry<String, IContextFactory> entry : contextFactories.entrySet())
            contexts.put(entry.getKey(), entry.getValue().createContext());
        
        LoadContext loadContext = new LoadContext(contexts, initParameters);

        try
        {
            loadRootElement(rootElement, loadContext);
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(messages.invalidConfiguration(rootElement.toString()), e);
        }
        
        return loadContext.createConfigurationContext();
    }

    protected void processRootElement(JsonObjectBuilder rootElement, boolean schema)
    {
    }

    private void createRootElementType(LoadContext loadContext)
    {
        JsonObjectBuilder rootElementType = new JsonObjectBuilder();
        rootElementType.put("instanceOf", "object");
        rootElementType.put("final", true);
        JsonObjectBuilder properties = new JsonObjectBuilder();
        rootElementType.put("properties", properties);
        
        for (Map.Entry<String, Pair<String, Boolean>> entry : topLevelElements.entrySet())
        {
            JsonObjectBuilder property = new JsonObjectBuilder();
            property.put("required", entry.getValue().getValue());
            property.put("type", entry.getValue().getKey());
            properties.put(entry.getKey(), property);
        }
        
        Object types = loadContext.schemaElement.get("types", null);
        if (!(types instanceof JsonObjectBuilder))
            return;
        
        ((JsonObjectBuilder)types).put("Configuration", rootElementType);
    }

    private void loadConfiguration(String configurationLocation, LoadContext loadContext, boolean schema, 
        IPropertyResolver propertyResolver, boolean checkProperties)
    {
        configurationLocation = configurationLocation.replace(File.separatorChar, '/');
        
        if (loadContext.loadedConfigurations.contains(configurationLocation))
            return;
            
        loadContext.loadedConfigurations.add(configurationLocation);
        
        String formatType = getFormatType(configurationLocation);

        try
        {
            InputStream stream = resourceLoader.getResource(configurationLocation);
            String contextPath = getContextPath(configurationLocation);
            
            if (propertyResolver == null)
            {
                List<IPropertyResolver> propertyResolvers = new ArrayList<IPropertyResolver>(this.propertyResolvers);
                propertyResolvers.add(new MapPropertyResolver(Collections.singletonMap("resource.path", contextPath)));
                propertyResolver = new CompositePropertyResolver(propertyResolvers);
            }

            IConfigurationParser configurationParser = configurationParsers.get(formatType);
            if (configurationParser == null)
            {
                configurationParser = configurationParsers.get("");
                if (configurationParser == null)
                    throw new InvalidConfigurationException(messages.unsupportedConfigurationFormat(configurationLocation, formatType));
            }

            String contents = expandProperties(configurationParser, propertyResolver, stream, "UTF-8", checkProperties);
            
            JsonObjectBuilder rootElement = configurationParser.parse(contents);
            processRootElement(rootElement, schema);
            JsonArrayBuilder imports = (JsonArrayBuilder)rootElement.remove("imports");
            
            if (!schema)
            {
                JsonArrayBuilder schemas = (JsonArrayBuilder)rootElement.remove("schemas");
                
                loadContext.rootElement = mergeObjects(loadContext.rootElement, rootElement, "");
                
                if (schemas != null)
                {
                    for (Object schemaName : schemas)
                    {
                        Pair<String, Boolean> pair = schemaMappings.get(schemaName);
                        if (pair == null)
                            throw new InvalidConfigurationException(messages.schemaLocationNotFound((String)schemaName));
                        
                        loadConfiguration(pair.getKey(), loadContext, true, propertyResolver, checkProperties);
                    }
                }
            }
            else
                loadContext.schemaElement = mergeSchemas(loadContext.schemaElement, rootElement);

            if (imports != null)
            {
                for (Object importElement : imports)
                    loadImport((String)importElement, contextPath, loadContext, schema, propertyResolver, checkProperties);
            }
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(messages.invalidConfiguration(configurationLocation), e);
        }
    }
    
    private JsonObjectBuilder mergeSchemas(JsonObjectBuilder schema1, JsonObjectBuilder schema2)
    {
        Assert.notNull(schema2);
        
        if (schema1 == null)
            return schema2;
        
        JsonObjectBuilder types1 = JsonUtils.get(schema1, "types", JsonObjectBuilder.class, null);
        if (types1 == null)
            return schema2;
        
        JsonObjectBuilder types2 = JsonUtils.get(schema2, "types", JsonObjectBuilder.class, null);
        if (types2 != null)
        {
            for (Map.Entry<String, Object> entry : types2)
            {
                if (types1.contains(entry.getKey()))
                    throw new InvalidConfigurationException(messages.typeAlreadyDefined(entry.getKey()));
                
                types1.put(entry.getKey(), entry.getValue());
            }
        }
        
        return schema1;
    }
    
    private JsonObjectBuilder mergeObjects(JsonObjectBuilder element1, JsonObjectBuilder element2, String path)
    {
        if (element1 == null)
            return element2;
        
        for (Map.Entry<String, Object> entry : element2)
        {
            String elementPath;
            if (!path.isEmpty())
                elementPath = path + '.' + entry.getKey();
            else
                elementPath = entry.getKey();
            
            Object value1 = element1.get(entry.getKey(), null);
            Object value2 = entry.getValue();
            if (value1 == null)
                element1.put(entry.getKey(), value2);
            else if (value1 instanceof JsonObjectBuilder && value2 instanceof JsonObjectBuilder)
                mergeObjects((JsonObjectBuilder)value1, (JsonObjectBuilder)value2, elementPath);
            else if (value1 instanceof JsonArrayBuilder && value2 instanceof JsonArrayBuilder)
            {
                JsonArrayBuilder array1 = (JsonArrayBuilder)value1;
                JsonArrayBuilder array2 = (JsonArrayBuilder)value2;
                for (Object element : array2)
                    array1.add(element);
            }
            else if (!Objects.equals(value1, value2))
                throw new InvalidConfigurationException(messages.incompatibleConfigurations(elementPath));
        }
        
        return element1;
    }

    private String getFormatType(String configurationLocation)
    {
        int pos = configurationLocation.lastIndexOf('.');
        if (pos == -1)
            return "";
        
        return configurationLocation.substring(pos + 1);
    }

    private String getContextPath(String configurationLocation)
    {
        int pos = configurationLocation.lastIndexOf('/');
        if (pos != -1)
            return configurationLocation.substring(0, pos);
        
        pos = configurationLocation.indexOf(':');
        if (pos != -1)
            return configurationLocation.substring(0, pos + 1);
        
        return "";
    }

    private void loadRootElement(JsonObject rootElement, ILoadContext loadContext)
    {
        for (Map.Entry<String, Object> entry : rootElement)
        {
            String elementName = entry.getKey();
            if (!elementName.equals("imports") && !elementName.equals("schemas"))
            {
                IElementLoader elementLoader = elementLoaders.get(entry.getKey());
                if (elementLoader != null)
                    elementLoader.loadElement((JsonObject)entry.getValue(), loadContext);
                else
                    throw new InvalidConfigurationException(messages.loaderNotFoundName(entry.getKey()));
            }
        }
    }
    
    private void loadImport(String resourceLocation, String contextPath, LoadContext loadContext, boolean schema,
        IPropertyResolver propertyResolver, boolean checkProperties)
    {
        if (contextPath != null &&
            (contextPath.indexOf(':') == -1 || contextPath.lastIndexOf('/') == (contextPath.length() - 1)))
            throw new InvalidArgumentException(messages.illegalContextPath(contextPath));

        if (resourceLocation.indexOf(":") == -1)
        {
            // Relative resource specified
            if (contextPath == null)
                // Context path must be set to resolve relative path
                throw new InvalidArgumentException(messages.contextPathNotSet(resourceLocation));
            if (resourceLocation.startsWith("/"))
                throw new InvalidArgumentException(messages.illegalResourceLocation(resourceLocation));
            
            resourceLocation = contextPath + "/" + resourceLocation; 
        }

        loadConfiguration(resourceLocation, loadContext, schema, propertyResolver, checkProperties);
    }
    
    private String expandProperties(IConfigurationParser parser, IPropertyResolver propertyResolver, InputStream stream, String charset, boolean check)
    {
        try
        {
            String contents = IOs.read(stream, charset);
            
            return Properties.expandProperties(parser, propertyResolver, contents, check, true);
        }
        catch (IOException e)
        {
            throw new InvalidConfigurationException(e);
        }
    }
    
    private static Parameters createDefaultParameters(Set<String> qualifiers)
    {
        Parameters parameters = new Parameters();
        
        CommonConfigurationExtention commonExtention = new CommonConfigurationExtention();
        parameters.combine(commonExtention.getParameters());
        
        List<IConfigurationLoaderExtension> extensions = Services.loadProviders(IConfigurationLoaderExtension.class, qualifiers);
        for (IConfigurationLoaderExtension extension : extensions)
            parameters.combine(extension.getParameters());
        
        parameters.resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        parameters.resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        parameters.resourceLoaders.put(InlineResourceLoader.SCHEMA, new InlineResourceLoader());
        
        parameters.propertyResolvers.add(new SystemPropertyResolver());
        
        parameters.configurationParsers.put("json",  new JsonConfigurationParser());
        parameters.configurationParsers.put("",  new JsonConfigurationParser());
        
        return parameters;
    }
    
    private static class LoadContext implements ILoadContext
    {
        private final Map<String, Object> contexts;
        private Map<String, Object> initParameters;
        private final Set<String> loadedConfigurations = new HashSet<String>();
        private JsonObjectBuilder rootElement;
        private JsonObjectBuilder schemaElement;
        
        public LoadContext(Map<String, Object> contexts, Map<String, Object> initParameters)
        {
            Assert.notNull(contexts);
            Assert.notNull(initParameters);
            
            this.contexts = contexts;
            this.initParameters = initParameters;
        }

        @Override
        public <T> T findParameter(String name)
        {
            Assert.notNull(initParameters);
            return (T)initParameters.get(name);
        }

        @Override
        public void setParameter(String name, Object value)
        {
            if (initParameters == null)
                initParameters = new HashMap<String, Object>();
            
            initParameters.put(name, value);
        }
        
        @Override
        public <T> T get(String configurationType)
        {
            return (T)contexts.get(configurationType);
        }
        
        public ILoadContext createConfigurationContext()
        {
            Map<String, Object> map = new LinkedHashMap<String, Object>(contexts.size());
            for (Map.Entry<String, Object> entry : contexts.entrySet())
            {
                Object configuration = ((IConfigurationFactory)entry.getValue()).createConfiguration(this);
                if (configuration != null)
                    map.put(entry.getKey(), configuration);
            }
            
            return new LoadContext(map, initParameters);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Configuration element loader is not found for element with name ''{0}''.")
        ILocalizedMessage loaderNotFoundName(String elementName);
        @DefaultMessage("Configuration element loader is not found for element with type ''{0}''.")
        ILocalizedMessage loaderNotFoundType(String elementType);
        @DefaultMessage("Type is not set for configuration element ''{0}''.")
        ILocalizedMessage typeNotSetForNestedElement(Object element);
        @DefaultMessage("Format ''{1}'' of configuration ''{0}'' is not supported.")
        ILocalizedMessage unsupportedConfigurationFormat(String configurationLocation, String formatType);
        @DefaultMessage("Configuration ''{0}'' is not valid.")
        ILocalizedMessage invalidConfiguration(String configurationLocation);
        @DefaultMessage("Illegal resource context path ''{0}''. Resource context path must start with '<schema>:' and must not end with '/'.")
        ILocalizedMessage illegalContextPath(String contextPath);
        @DefaultMessage("Illegal resource location ''{0}''. Absolute resource location must start with '<schema>:', relative resource location must not start with '<schema>:' and '/'.")
        ILocalizedMessage illegalResourceLocation(String location);
        @DefaultMessage("Context path is not set to resolve relative location ''{0}''.")
        ILocalizedMessage contextPathNotSet(String location);
        @DefaultMessage("Root element of configuration ''{0}'' is not found.")
        ILocalizedMessage rootElementNotFound(String configurationLocation);
        @DefaultMessage("Schema of configuration ''{0}'' is not found.")
        ILocalizedMessage schemaNotFound(String configurationLocation);
        @DefaultMessage("Location of schema ''{0}'' is not found.")
        ILocalizedMessage schemaLocationNotFound(String schemaName);
        @DefaultMessage("Could not create combined schema. Type ''{0}'' is already defined.")
        ILocalizedMessage typeAlreadyDefined(String typeName);
        @DefaultMessage("Could not create combined configuration. Configurations have incompatible elements on path ''{0}''.")
        ILocalizedMessage incompatibleConfigurations(String path);
    }
}
