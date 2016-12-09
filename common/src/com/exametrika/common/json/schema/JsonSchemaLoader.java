/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;



/**
 * The {@link JsonSchemaLoader} is a loader of JSON schemas.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonSchemaLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<String, IJsonValidator> validators;
    private final Map<String, IJsonConverter> converters;
    private final boolean addBuiltIns;
    
    public JsonSchemaLoader()
    {
        this(Collections.<String, IJsonValidator>emptyMap(), Collections.<String, IJsonConverter>emptyMap(), true);
    }
    
    public JsonSchemaLoader(Map<String, IJsonValidator> validators, Map<String, IJsonConverter> converters, boolean addBuiltIns)
    {
        Assert.notNull(validators);
        Assert.notNull(converters);

        if (addBuiltIns)
            converters = addDefaultConverters(new HashMap<String, IJsonConverter>(converters));

        this.validators = validators;
        this.converters = converters;
        this.addBuiltIns = addBuiltIns;
    }

    public JsonSchema loadSchema(JsonObject element)
    {
        String name = element.get("name", "");
        String description = element.get("description", "");
        
        List<JsonType> types = new ArrayList<JsonType>();
        Map<String, JsonType> typesMap = new HashMap<String, JsonType>();
        
        if (addBuiltIns)
            addBuiltInTypes(types);
        
        for (JsonType type : types)
            typesMap.put(type.getName(), type);
        
        if (element.contains("types"))
        {
            JsonObject typesElement = element.get("types");
            for (Map.Entry<String, Object> entry : typesElement)
            {
                JsonType type = typesMap.get(entry.getKey());
                if (type == null)
                    type = loadType(entry.getKey(), (JsonObject)entry.getValue(), typesMap, typesElement, true);
                
                types.add(type);
            }
        }
        
        return new JsonSchema(name, description, types);
    }

    private void addBuiltInTypes(List<JsonType> types)
    {
        JsonType anyType = new JsonAnyType("any", "", null, null, null, null);
        types.add(anyType);
        
        JsonStringType stringType = new JsonStringType("string", "", null, null, null, null, null, 0, Integer.MAX_VALUE);
        types.add(stringType);
        
        JsonArrayType arrayType = new JsonArrayType("array", "", null, null, null, null, 0, Integer.MAX_VALUE, true, false);
        arrayType.setElementType(anyType);
        types.add(arrayType);
        
        JsonMapType mapType = new JsonMapType("map", "", null, null, null, null, 0, Integer.MAX_VALUE, false);
        mapType.setKeyType(stringType);
        mapType.setValueType(anyType);
        types.add(mapType);
        
        types.add(new JsonBooleanType("boolean", "", null));
        types.add(new JsonDoubleType("double", "", null, null, null, null, -Double.MAX_VALUE, false, Double.MAX_VALUE, false));
        types.add(new JsonLongType("long", "", null, null, null, null, Long.MIN_VALUE, false, Long.MAX_VALUE, false));
        
        JsonObjectType objectType = new JsonObjectType("object", "", null, null, null, null, true, false, true);
        objectType.setProperties(Collections.<JsonProperty>emptyList());
        objectType.setBase(null);
        types.add(objectType);
    }

    private JsonType loadType(String name, JsonObject typeElement, Map<String, JsonType> typesMap, JsonObject typesElement, boolean topLevel)
    {
        String typeName = getType(typeElement);
        
        String description = typeElement.get("description", "");
        JsonArray enumeration = typeElement.get("enumeration", null);
        JsonObject annotation = typeElement.get("annotation", null);
        List<IJsonValidator> validators = loadValidators(typeElement);
        IJsonConverter converter = loadConverter(typeElement);
        
        if (typeName.equals("any"))
        {
            JsonAnyType anyType = new JsonAnyType(name, description, enumeration, annotation, validators, converter);
            
            if (topLevel)
                typesMap.put(name, anyType);
            
            return anyType;
        }
        else if (typeName.equals("array"))
        {
            long minCount = typeElement.get("minCount", 0); 
            long maxCount = loadMax(typeElement.get("maxCount", Integer.MAX_VALUE));
                
            boolean allowDuplicates = typeElement.get("allowDuplicates", false);
            boolean allowNulls = typeElement.get("allowNulls", false);
            
            JsonArrayType arrayType = new JsonArrayType(name, description, enumeration, annotation, validators,
                converter, (int)minCount, (int)maxCount, allowDuplicates, allowNulls);
            
            if (topLevel)
                typesMap.put(name, arrayType);
            
            arrayType.setElementType(loadInnerType(typeElement.get("elementType"), typesMap, typesElement));
            
            return arrayType;
        }
        else if (typeName.equals("map"))
        {
            long minCount = typeElement.get("minCount", 0); 
            long maxCount = loadMax(typeElement.get("maxCount", Integer.MAX_VALUE));
                
            boolean allowNulls = typeElement.get("allowNulls", false);
            
            JsonMapType mapType = new JsonMapType(name, description, enumeration, annotation, validators,
                converter, (int)minCount, (int)maxCount, allowNulls);
            
            if (topLevel)
                typesMap.put(name, mapType);
            
            Object keyTypeElement = typeElement.get("keyType", null);
            JsonStringType keyType;
            if (keyTypeElement != null)
                keyType = (JsonStringType)loadInnerType(keyTypeElement, typesMap, typesElement);
            else
                keyType =  new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE);
            
            mapType.setKeyType(keyType);
            mapType.setValueType(loadInnerType(typeElement.get("valueType"), typesMap, typesElement));
            
            return mapType;
        }
        else if (typeName.equals("boolean"))
        {
            JsonBooleanType booleanType = new JsonBooleanType(name, description, converter);
            if (topLevel)
                typesMap.put(name, booleanType);
            
            return booleanType;
        }
        else if (typeName.equals("compound"))
        {
            JsonCompoundType compoundType = new JsonCompoundType(name, description, enumeration, annotation, validators,
                converter);
            
            if (topLevel)
                typesMap.put(name, compoundType);
            
            JsonArray compoundTypesElement = typeElement.get("types");
            List<JsonType> types = new ArrayList<JsonType>(compoundTypesElement.size());
            for (Object type : compoundTypesElement)
                types.add(loadInnerType(type, typesMap, typesElement));
            
            compoundType.setTypes(types);
            
            return compoundType;
        }
        else if (typeName.equals("double"))
        {
            double min = typeElement.get("min", -Double.MAX_VALUE); 
            double max = typeElement.get("max", Double.MAX_VALUE);
            boolean minExclusive = typeElement.get("minExclusive", false);
            boolean maxExclusive = typeElement.get("maxExclusive", false);
            
            JsonDoubleType doubleType = new JsonDoubleType(name, description, enumeration, annotation, validators, 
                converter, min, minExclusive, max, maxExclusive);
            
            if (topLevel)
                typesMap.put(name, doubleType);
            
            return doubleType;
        }
        else if (typeName.equals("long"))
        {
            long min = typeElement.get("min", Long.MIN_VALUE); 
            long max = typeElement.get("max", Long.MAX_VALUE);
            boolean minExclusive = typeElement.get("minExclusive", false);
            boolean maxExclusive = typeElement.get("maxExclusive", false);
            
            JsonLongType longType = new JsonLongType(name, description, enumeration, annotation, validators, 
                converter, min, minExclusive, max, maxExclusive);
            
            if (topLevel)
                typesMap.put(name, longType);
            
            return longType;
        }
        else if (typeName.equals("object"))
        {
            boolean open = typeElement.get("open", false);
            boolean isAbstract = typeElement.get("abstract", false);
            boolean isFinal = typeElement.get("final", false);
            
            JsonObjectType objectType = new JsonObjectType(name, description, enumeration, annotation, validators, 
                converter, open, isAbstract, isFinal);
            
            if (topLevel)
                typesMap.put(name, objectType);
            
            List<JsonProperty> properties = loadProperties((JsonObject)typeElement.get("properties", null), typesMap, typesElement);
            objectType.setProperties(properties);
            
            String baseName = typeElement.get("base", null);
            JsonObjectType base = null;
            if (baseName != null)
                base = (JsonObjectType)loadInnerType(baseName, typesMap, typesElement);
            objectType.setBase(base);
            
            return objectType;
        }
        else if (typeName.equals("string"))
        {
            String pattern = typeElement.get("pattern", null);
            
            long minCount = typeElement.get("minCount", 0); 
            long maxCount = loadMax(typeElement.get("maxCount", Integer.MAX_VALUE));
            
            JsonStringType stringType = new JsonStringType(name, description, enumeration, annotation, validators, 
                converter, pattern, (int)minCount, (int)maxCount);
            
            if (topLevel)
                typesMap.put(name, stringType);
            
            return stringType;
        }
        else
            throw new InvalidConfigurationException(messages.invalidSchemaType(typeName));
    }
    
    private long loadMax(Object element)
    {
        if (element instanceof Long)
            return (Long)element;
        else if (element.equals("unbounded"))
            return Integer.MAX_VALUE;
        else
            return Assert.error();
    }
    
    private List<JsonProperty> loadProperties(JsonObject element, Map<String, JsonType> typesMap, JsonObject typesElement)
    {
        if (element == null)
            return Collections.emptyList();

        List<JsonProperty> properties = new ArrayList<JsonProperty>(element.size());
        for (Map.Entry<String, Object> entry : element)
        {
            JsonObject child = (JsonObject)entry.getValue();
            String description = child.get("description", "");
            JsonType type = loadInnerType(child.get("type"), typesMap, typesElement);
            boolean required = child.get("required", true);
            boolean allowed = child.get("allowed", true);
            Object defaultValue = child.get("default", null);
            properties.add(new JsonProperty(entry.getKey(), description, type, required, allowed, defaultValue));
        }
        
        return properties;
    }

    private JsonType loadInnerType(Object element, Map<String, JsonType> typesMap, JsonObject typesElement)
    {
        if (element instanceof String)
        {
            String typeName = (String)element;
            JsonType type = typesMap.get(typeName);
            if (type == null)
            {
                JsonObject typeElement = typesElement.get(typeName, null);
                if (typeElement == null)
                    throw new InvalidConfigurationException(messages.typeNotFound(typeName));
                
                type = loadType(typeName, typeElement, typesMap, typesElement, true);
            }
            return type;
        }
        else if (element instanceof JsonObject)
            return loadType("", (JsonObject)element, typesMap, typesElement, false);
        else
            return Assert.error();
    }

    private List<IJsonValidator> loadValidators(JsonObject element)
    {
        JsonArray configurations = element.get("validators", null);
        if (configurations != null)
        {
            List<IJsonValidator> validators = new ArrayList<IJsonValidator>(configurations.size());
            for (Object configuration : configurations)
            {
                IJsonValidator validator = this.validators.get(configuration);
                if (validator == null)
                    throw new InvalidConfigurationException(messages.validatorNotFound((String)configuration));
                else
                    validators.add(validator);
            }
            
            return validators;
        }
        else
            return null;
    }
    
    private IJsonConverter loadConverter(JsonObject element)
    {
        String configuration = element.get("converter", null);
        if (configuration != null)
        {
            IJsonConverter converter = this.converters.get(configuration);
            if (converter == null)
                throw new InvalidConfigurationException(messages.converterNotFound(configuration));

            return converter;
        }
        else
            return null;
    }

    private String getType(JsonObject element)
    {
        if (element.contains("instanceOf"))
            return element.get("instanceOf");
        else
            throw new InvalidConfigurationException(messages.typeNotSet(Strings.indent(element.toString(), 4)));
    }

    private static Map<String, IJsonConverter> addDefaultConverters(Map<String, IJsonConverter> converters)
    {
        converters.put("booleans", new JsonBooleanConverter());
        converters.put("bytes", new JsonByteLongConverter());
        converters.put("periods", new JsonPeriodLongConverter());
        converters.put("percents", new JsonPercentDoubleConverter());
        return converters;
    }

    private interface IMessages
    {
        @DefaultMessage("Type ''{0}'' of schema element is invalid.")
        ILocalizedMessage invalidSchemaType(String typeName);
        
        @DefaultMessage("Type ''{0}'' is not found.")
        ILocalizedMessage typeNotFound(String typeName);
        
        @DefaultMessage("Type is not set for schema element: \n{0}.")
        ILocalizedMessage typeNotSet(String element);
        
        @DefaultMessage("Validator ''{0}'' is not found.")
        ILocalizedMessage validatorNotFound(String validator);
        
        @DefaultMessage("Converter ''{0}'' is not found.")
        ILocalizedMessage converterNotFound(String converter);
    }
}
