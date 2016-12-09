/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.json.JsonException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.IOs;




/**
 * The {@link JsonMetaSchemaFactory} is a factory of JSON meta schema.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class JsonMetaSchemaFactory
{
    public JsonSchema createMetaSchema(Set<String> validatorNames)
    {
        Map<String, IJsonValidator> validators = new HashMap<String, IJsonValidator>();
        validators.put("type", new JsonTypeValidator(validatorNames));
        validators.put("typeReference", new JsonTypeReferenceValidator(null, 
            Collections.asSet("any", "boolean", "long", "double", "string", "array", "map", "object")));
        validators.put("minMaxCount", new JsonMinMaxCountValidator());
        validators.put("regExp", new JsonRegExpValidator());
        validators.put("stringTypeReference", new JsonTypeReferenceValidator("string", Collections.asSet("string")));
        validators.put("property", new JsonPropertyValidator());
        validators.put("object", new JsonObjectTypeValidator());
        validators.put("objectTypeReference", new JsonTypeReferenceValidator("object", Collections.asSet("object")));
        validators.put("schema", new JsonSchemaValidator());
        
        JsonSchemaLoader loader = new JsonSchemaLoader(validators, java.util.Collections.<String, IJsonConverter>emptyMap(), false);
        Reader reader = null;
        try
        {
            reader = new InputStreamReader(Classes.getResource(JsonSchema.class, "schema.schema"), "UTF-8");
            JsonObject metaObject = (JsonObject)JsonSerializers.read(reader, false);
            return loader.loadSchema(metaObject);
        }
        catch (Exception e)
        {
            throw new JsonException(e);
        }
        finally
        {
            IOs.close(reader);
        }
    }
}
