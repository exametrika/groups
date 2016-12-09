/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.json;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.json.schema.IJsonConverter;
import com.exametrika.common.json.schema.IJsonValidationContext;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonAnyType;
import com.exametrika.common.json.schema.JsonArrayType;
import com.exametrika.common.json.schema.JsonBooleanConverter;
import com.exametrika.common.json.schema.JsonBooleanType;
import com.exametrika.common.json.schema.JsonByteLongConverter;
import com.exametrika.common.json.schema.JsonCompoundType;
import com.exametrika.common.json.schema.JsonDiagnostics;
import com.exametrika.common.json.schema.JsonDoubleType;
import com.exametrika.common.json.schema.JsonLongType;
import com.exametrika.common.json.schema.JsonMapType;
import com.exametrika.common.json.schema.JsonMetaSchemaFactory;
import com.exametrika.common.json.schema.JsonMinMaxCountValidator;
import com.exametrika.common.json.schema.JsonObjectType;
import com.exametrika.common.json.schema.JsonObjectTypeValidator;
import com.exametrika.common.json.schema.JsonPathReferenceValidator;
import com.exametrika.common.json.schema.JsonPercentDoubleConverter;
import com.exametrika.common.json.schema.JsonPeriodLongConverter;
import com.exametrika.common.json.schema.JsonProperty;
import com.exametrika.common.json.schema.JsonPropertyValidator;
import com.exametrika.common.json.schema.JsonRegExpValidator;
import com.exametrika.common.json.schema.JsonSchema;
import com.exametrika.common.json.schema.JsonSchemaLoader;
import com.exametrika.common.json.schema.JsonSchemaValidator;
import com.exametrika.common.json.schema.JsonStringType;
import com.exametrika.common.json.schema.JsonType;
import com.exametrika.common.json.schema.JsonTypeReferenceValidator;
import com.exametrika.common.json.schema.JsonTypeValidator;
import com.exametrika.common.json.schema.JsonValidationContext;
import com.exametrika.common.json.schema.JsonValidationException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Objects;


/**
 * The {@link JsonSchemaTests} are tests for JSON schema.
 * 
 * @author Medvedev-A
 */
public class JsonSchemaTests
{
    private static final IMessages messages = Messages.get(IMessages.class);

    @Test
    public void testAnyType()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        assertThat(anyType.supports(new Object()), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        anyType.validate("Test", new JsonValidationContext(schema, diagnostics, anyType, "Test"));
        assertThat(diagnostics.isValid(), is(true));
        
        JsonArrayBuilder enumeration = new JsonArrayBuilder();
        enumeration.add("aa");
        enumeration.add(10);
        enumeration.add(true);
        anyType = new JsonAnyType("any", "any type", enumeration, null, null, null);
        
        diagnostics = new JsonDiagnostics();
        anyType.validate("aa", new JsonValidationContext(schema, diagnostics, anyType, "aa"));
        anyType.validate(10l, new JsonValidationContext(schema, diagnostics, anyType, 10l));
        anyType.validate(true, new JsonValidationContext(schema, diagnostics, anyType, true));
        assertThat(diagnostics.isValid(), is(true));
        
        anyType.validate("Test", new JsonValidationContext(schema, diagnostics, anyType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        TestValidator1 validator1 = new TestValidator1(Arrays.<Class>asList(String.class), Arrays.<Object>asList("test"), "Instance is not a 'test'.");
        TestValidator1 validator2 = new TestValidator1(Arrays.<Class>asList(Integer.class), Arrays.<Object>asList(123), "Instance is not a '123'.");
        anyType = new JsonAnyType("any", "any type", null, null, Arrays.<IJsonValidator>asList(validator1, validator2), null);
        
        diagnostics = new JsonDiagnostics();
        anyType.validate("test", new JsonValidationContext(schema, diagnostics, anyType, "test"));
        anyType.validate(123, new JsonValidationContext(schema, diagnostics, anyType, 123));
        assertThat(diagnostics.isValid(), is(true));
        
        anyType.validate("Hello", new JsonValidationContext(schema, diagnostics, anyType, "Hello"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        anyType.validate(567, new JsonValidationContext(schema, diagnostics, anyType, 567));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        anyType.validate(new Object(), new JsonValidationContext(schema, diagnostics, anyType, new Object()));
        assertThat(diagnostics.isValid(), is(true));
    }
    
    @Test
    public void testArrayType()
    {
        JsonArrayType arrayType = new JsonArrayType("array", "array type", null, null, null, null, 1, 2, false, false);
        arrayType.setElementType(new JsonStringType("string", "string type", null, null, null, null, null, 0, 5));
        arrayType.freeze();
        assertThat(arrayType.supports(new Object()), is(false));
        assertThat(arrayType.supports(JsonUtils.EMPTY_ARRAY), is(true));
        assertThat(arrayType.supports(new JsonArrayBuilder()), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        arrayType.validate("Test", new JsonValidationContext(schema, diagnostics, arrayType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        JsonArrayBuilder array = new JsonArrayBuilder();
        array.add("test1");
        array.add("test2");
        diagnostics = new JsonDiagnostics();
        arrayType.validate(array, new JsonValidationContext(schema, diagnostics, arrayType, array));
        assertThat(diagnostics.isValid(), is(true));
        
        array.add(123);
        array.add("test1");
        array.add(null);
        array.add("test123");
        diagnostics = new JsonDiagnostics();
        arrayType.validate(array, new JsonValidationContext(schema, diagnostics, arrayType, array));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(5));
    }
    
    @Test
    public void testBooleanType()
    {
        JsonBooleanType booleanType = new JsonBooleanType("boolean", "boolean type", null);
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        assertThat(booleanType.supports(true), is(true));
        assertThat(booleanType.supports("true"), is(true));
        
        booleanType.validate(true, new JsonValidationContext(schema, diagnostics, booleanType, true));
        assertThat(diagnostics.isValid(), is(true));
        
        booleanType.validate("Test", new JsonValidationContext(schema, diagnostics, booleanType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testCompoundType()
    {
        JsonType type1 = new JsonStringType("string", "string type", null, null, null, null, null, 0, 5);
        JsonType type2 = new JsonLongType("long", "long type", null, null, null, null, 0, false, 10, false);
        JsonCompoundType compoundType = new JsonCompoundType("compound", "compound type", null, null, null, null);
        compoundType.setTypes(Arrays.asList(type1, type2));
        compoundType.freeze();
        assertThat(compoundType.supports(new Object()), is(false));
        assertThat(compoundType.supports("test"), is(true));
        assertThat(compoundType.supports(123), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();

        compoundType.validate("Test", new JsonValidationContext(schema, diagnostics, compoundType, "Test"));
        assertThat(diagnostics.isValid(), is(true));
        
        compoundType.validate("long Test", new JsonValidationContext(schema, diagnostics, compoundType, "long Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        compoundType.validate(5l, new JsonValidationContext(schema, diagnostics, compoundType, "5l"));
        assertThat(diagnostics.isValid(), is(true));
        
        compoundType.validate(100l, new JsonValidationContext(schema, diagnostics, compoundType, 100l));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        compoundType.validate(true, new JsonValidationContext(schema, diagnostics, compoundType, true));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testDoubleType()
    {
        JsonType doubleType = new JsonDoubleType("double", "double type", null, null, null, null, 5d, false, 10d, false);
        assertThat(doubleType.supports(10.1d), is(true));
        assertThat(doubleType.supports("10.1"), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();

        doubleType.validate(5d, new JsonValidationContext(schema, diagnostics, doubleType, 5d));
        doubleType.validate(10d, new JsonValidationContext(schema, diagnostics, doubleType, 10d));
        assertThat(diagnostics.isValid(), is(true));
        
        doubleType.validate(4d, new JsonValidationContext(schema, diagnostics, doubleType, 4d));
        doubleType.validate(11d, new JsonValidationContext(schema, diagnostics, doubleType, 11d));
        doubleType.validate("Test", new JsonValidationContext(schema, diagnostics, doubleType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(3));
        
        diagnostics = new JsonDiagnostics();
        doubleType = new JsonDoubleType("double", "double type", null, null, null, null, 5d, true, 10d, true);
        doubleType.validate(6d, new JsonValidationContext(schema, diagnostics, doubleType, 6d));
        doubleType.validate(9d, new JsonValidationContext(schema, diagnostics, doubleType, 9d));
        assertThat(diagnostics.isValid(), is(true));
        
        doubleType.validate(5d, new JsonValidationContext(schema, diagnostics, doubleType, 5d));
        doubleType.validate(10d, new JsonValidationContext(schema, diagnostics, doubleType, 10d));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testLongType()
    {
        JsonType longType = new JsonLongType("long", "long type", null, null, null, null, 5, false, 10, false);
        assertThat(longType.supports(10l), is(true));
        assertThat(longType.supports("0x10"), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();

        longType.validate(5l, new JsonValidationContext(schema, diagnostics, longType, 5l));
        longType.validate(10l, new JsonValidationContext(schema, diagnostics, longType, 10l));
        assertThat(diagnostics.isValid(), is(true));
        
        longType.validate(4l, new JsonValidationContext(schema, diagnostics, longType, 4l));
        longType.validate(11l, new JsonValidationContext(schema, diagnostics, longType, 11l));
        longType.validate("Test", new JsonValidationContext(schema, diagnostics, longType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(3));
        
        diagnostics = new JsonDiagnostics();
        longType = new JsonLongType("long", "long type", null, null, null, null, 5, true, 10, true);
        longType.validate(6l, new JsonValidationContext(schema, diagnostics, longType, 6l));
        longType.validate(9l, new JsonValidationContext(schema, diagnostics, longType, 9l));
        assertThat(diagnostics.isValid(), is(true));
        
        longType.validate(5l, new JsonValidationContext(schema, diagnostics, longType, 5l));
        longType.validate(10l, new JsonValidationContext(schema, diagnostics, longType, 10l));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testMapType()
    {
        JsonMapType mapType = new JsonMapType("map", "map type", null, null, null, null, 1, 2, false);
        mapType.setKeyType(new JsonStringType("string", "string type", null, null, null, null, null, 0, 5));
        mapType.setValueType(new JsonStringType("string", "string type", null, null, null, null, null, 0, 5));
        mapType.freeze();
        assertThat(mapType.supports(new Object()), is(false));
        assertThat(mapType.supports(JsonUtils.EMPTY_OBJECT), is(true));
        assertThat(mapType.supports(new JsonObjectBuilder()), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        mapType.validate("Test", new JsonValidationContext(schema, diagnostics, mapType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("test1", "test1");
        object.put("test2", "test2");
        diagnostics = new JsonDiagnostics();
        mapType.validate(object, new JsonValidationContext(schema, diagnostics, mapType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        object.put("test3", 123);
        object.put("test123", "test123");
        object.put("test4", null);
        diagnostics = new JsonDiagnostics();
        mapType.validate(object, new JsonValidationContext(schema, diagnostics, mapType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(5));
    }
    
    @Test
    public void testObjectType()
    {
        JsonProperty property1 = new JsonProperty("prop1", "prop1 property", new JsonStringType("string", "string type", null, null, null, null, null, 0, 5), 
            true, true, null);
        JsonProperty property2 = new JsonProperty("prop2", "prop2 property", new JsonLongType("long", "long type", null, null, null, null, 5, false, 10, false), 
            false, true, 7);
        JsonProperty property3 = new JsonProperty("prop3", "prop3 property", new JsonLongType("long", "long type", null, null, null, null, 5, false, 10, false), 
            false, true, null);
        JsonObjectType objectType = new JsonObjectType("object", "object type", null, null, null, null, false, false, false);
        objectType.setProperties(Arrays.asList(property1, property2, property3));
        objectType.setBase(null);
        objectType.freeze();
        assertThat(objectType.supports(new Object()), is(false));
        assertThat(objectType.supports(JsonUtils.EMPTY_OBJECT), is(true));
        assertThat(objectType.supports(new JsonObjectBuilder()), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        objectType.validate("Test", new JsonValidationContext(schema, diagnostics, objectType, "Test"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("instanceOf", "object");
        object.put("prop1", "test1");
        object.put("prop2", 10);
        object.put("prop3", 8);
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        object.remove("prop2");
        object.remove("prop3");
        
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        assertThat((Long)object.get("prop2"), is(7l));
        
        object.put("prop1", "test123");
        object.put("prop2", true);
        object.put("prop3", 9);
        object.put("prop4", 8);
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(3));
        
        diagnostics = new JsonDiagnostics();
        object.clear();
        object.put("prop1", null);
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        objectType = new JsonObjectType("object", "object type", null, null, null, null, true, false, false);
        objectType.setProperties(Arrays.asList(property1, property2, property3));
        objectType.freeze();
        object.clear();
        object.put("prop1", "test1");
        object.put("prop2", 10);
        object.put("prop3", 8);
        object.put("prop4", 8);
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        objectType = new JsonObjectType("object", "object type", null, null, null, null, false, true, false);
        objectType.setProperties(Arrays.asList(property1, property2, property3));
        objectType.freeze();
        object.clear();
        object.put("prop1", "test1");
        object.put("prop2", 10);
        object.put("prop3", 8);
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        object.clear();
        object.put("instanceOf", "test");
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        JsonObjectType objectType1 = new JsonObjectType("test", "test object type", null, null, null, null, false, false, false);
        objectType1.setProperties(Arrays.asList(property1, property2, property3));
        schema = new JsonSchema("", "", Arrays.<JsonType>asList(objectType1));
        objectType = new JsonObjectType("object", "object type", null, null, null, null, false, false, false);
        objectType.setProperties(Arrays.asList(property1, property2, property3));
        
        diagnostics = new JsonDiagnostics();
        objectType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        JsonObjectType baseType = new JsonObjectType("base", "base type", null, null, null, null, false, true, false);
        baseType.setProperties(Arrays.asList(property1, property2));
        
        JsonProperty property22 = new JsonProperty("prop2", "prop2 property", new JsonBooleanType("boolean", "boolean type", null), false, false, true);
        JsonProperty property4 = new JsonProperty("prop4", "prop4 property", new JsonBooleanType("boolean", "boolean type", null), false, false, null);
        objectType = new JsonObjectType("object", "object type", null, null, null, null, false, false, false);
        objectType.setProperties(Arrays.asList(property1, property22, property3, property4));
        objectType.setBase(baseType);
        schema = new JsonSchema("", "", Arrays.<JsonType>asList(baseType, objectType));
        
        diagnostics = new JsonDiagnostics();
        object = new JsonObjectBuilder();
        object.put("instanceOf", "object");
        object.put("prop1", "test1");
        object.put("prop3", 8);
        baseType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        assertThat((Boolean)object.get("prop2"), is(true));
        
        object.put("prop4", true);
        object.put("prop2", true);
        baseType.validate(object, new JsonValidationContext(schema, diagnostics, objectType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testStringType()
    {
        JsonType stringType = new JsonStringType("string", "string type", null, null, null, null, null, 5, 10);
        assertThat(stringType.supports("test"), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();

        stringType.validate("test1", new JsonValidationContext(schema, diagnostics, stringType, "test1"));
        stringType.validate("test1test1", new JsonValidationContext(schema, diagnostics, stringType, "test1test1"));
        assertThat(diagnostics.isValid(), is(true));
        
        stringType.validate("test", new JsonValidationContext(schema, diagnostics, stringType, "test"));
        stringType.validate("test1test11", new JsonValidationContext(schema, diagnostics, stringType, "test1test11"));
        stringType.validate(123, new JsonValidationContext(schema, diagnostics, stringType, 123));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(3));
        
        diagnostics = new JsonDiagnostics();
        stringType = new JsonStringType("string", "string type", null, null, null, null, "[0-9]*", 0, Integer.MAX_VALUE);
        stringType.validate("123", new JsonValidationContext(schema, diagnostics, stringType, "123"));
        assertThat(diagnostics.isValid(), is(true));
        
        stringType.validate("test1", new JsonValidationContext(schema, diagnostics, stringType, "test1"));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testSchema() throws Throwable
    {
        JsonType stringType = new JsonStringType("string", "string type", null, null, null, null, null, 5, 10);
        JsonType longType = new JsonLongType("long", "long type", null, null, null, null, 5, false, 10, false);
        
        final JsonSchema schema = new JsonSchema("schema", "", Arrays.<JsonType>asList(stringType, longType));
        assertThat(schema.findType("string") == stringType, is(true));
        assertThat(schema.findType("test"), nullValue());
        
        schema.validate("test1", "string");
        
        new Expected(JsonValidationException.class, new Runnable()
        {
            @Override
            public void run()
            {
                schema.validate("test", "string");
            }
        });
        
        new Expected(JsonValidationException.class, new Runnable()
        {
            @Override
            public void run()
            {
                schema.validate("test1", "test");
            }
        });
    }
    
    @Test
    public void testSchemaLoader() throws Throwable
    {   
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(Collections.<String>singleton("test"));
        JsonSchemaLoader loader = new JsonSchemaLoader(Collections.<String, IJsonValidator>singletonMap("test", new TestValidator2()), 
            Collections.<String, IJsonConverter>emptyMap(), true);
        JsonObjectBuilder object = (JsonObjectBuilder)JsonSerializers.read(new InputStreamReader(Classes.getResource(getClass(), "test-schema.json"), "UTF-8"), true);
        metaSchema.validate(object, "schema");
        JsonSchema schema = loader.loadSchema(object);
        
        List<JsonType> types = new ArrayList<JsonType>();
        
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
        types.add(objectType);
        
        JsonArrayBuilder enumeration = new JsonArrayBuilder();
        enumeration.add("hello");
        enumeration.add(10);
        enumeration.add(true);
        
        JsonObjectBuilder annotation = new JsonObjectBuilder();
        annotation.put("key1", "value1");
        annotation.put("key2", 123);
        annotation.put("key3", true);
        
        types.add(new JsonAnyType("testAny", "test any", enumeration, annotation, Arrays.<IJsonValidator>asList(new TestValidator2()), null));
        JsonType testAny2 = new JsonAnyType("testAny2", "", null, null, null, null);
        types.add(testAny2);
        JsonArrayType testArray = new JsonArrayType("testArray", "test array", null, null, null, null, 1, 2, false, false);
        testArray.setElementType(testAny2);
        types.add(testArray);
        JsonArrayType testArray2 = new JsonArrayType("testArray2", "", null, null, null, null, 0, Integer.MAX_VALUE, false, false);
        testArray2.setElementType(new JsonBooleanType("", "", null));
        types.add(testArray2);
        JsonType testBoolean = new JsonBooleanType("testBoolean", "test boolean", null);
        types.add(testBoolean);
        types.add(new JsonLongType("testLong", "test long", null, null, null, null, 0, true, 100, true));
        JsonType testLong2 = new JsonLongType("testLong2", "", null, null, null, null, Long.MIN_VALUE, false, Long.MAX_VALUE, false);
        types.add(testLong2);
        types.add(new JsonDoubleType("testDouble", "test double", null, null, null, null, 1d, true, 100.1d, true));
        types.add(new JsonDoubleType("testDouble2", "", null, null, null, null, -Double.MAX_VALUE, false, Double.MAX_VALUE, false));
        types.add(new JsonStringType("testString", "test string", null, null, null, null, "[0-9, a-f]", 1, 100));
        JsonStringType testString2 = new JsonStringType("testString2", "", null, null, null, null, null, 0, Integer.MAX_VALUE);
        types.add(testString2);
        JsonMapType testMap = new JsonMapType("testMap", "test map", null, null, null, null, 1, 2, true);
        testMap.setKeyType(testString2);
        testMap.setValueType(testAny2);
        types.add(testMap);
        JsonMapType testMap2 = new JsonMapType("testMap2", "", null, null, null, null, 0, Integer.MAX_VALUE, false);
        testMap2.setKeyType(new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE));
        testMap2.setValueType(new JsonBooleanType("", "", null));
        types.add(testMap2);
        JsonMapType testMap3 = new JsonMapType("testMap3", "", null, null, null, null, 0, Integer.MAX_VALUE, false);
        testMap3.setKeyType(new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE));
        testMap3.setValueType(new JsonBooleanType("", "", null));
        types.add(testMap3);
        JsonCompoundType testCompound = new JsonCompoundType("testCompound", "test compound", null, null, null, null);
        testCompound.setTypes(Arrays.asList(testString2, testLong2, testBoolean));
        types.add(testCompound);
        JsonCompoundType testCompound2 = new JsonCompoundType("testCompound2", "", null, null, null, null);
        testCompound2.setTypes(Arrays.asList(new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE), 
            new JsonLongType("", "", null, null, null, null, Long.MIN_VALUE, false, Long.MAX_VALUE, false), new JsonBooleanType("", "", null)));
        types.add(testCompound2);
        
        List<JsonProperty> properties1 = new ArrayList<JsonProperty>();
        properties1.add(new JsonProperty("prop1", "prop1 property", testLong2, true, true, null));
        properties1.add(new JsonProperty("prop2", "", new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE), false, true, "hello"));
        properties1.add(new JsonProperty("prop3", "", new JsonStringType("", "", null, null, null, null, null, 0, Integer.MAX_VALUE), false, true, null));
        properties1.add(new JsonProperty("prop4", "", schema.findType("boolean"), true, true, null));
        properties1.add(new JsonProperty("prop5", "", schema.findType("any"), true, true, null));
        properties1.add(new JsonProperty("prop6", "", schema.findType("long"), true, true, null));
        properties1.add(new JsonProperty("prop7", "", schema.findType("double"), true, true, null));
        properties1.add(new JsonProperty("prop8", "", schema.findType("map"), true, true, null));
        properties1.add(new JsonProperty("prop9", "", schema.findType("array"), true, true, null));
        properties1.add(new JsonProperty("prop10", "", schema.findType("object"), true, true, null));
        properties1.add(new JsonProperty("prop11", "", schema.findType("string"), true, true, null));
        JsonObjectType testObject = new JsonObjectType("testObject", "test object", null, null, null, null, true, true, false);
        testObject.setProperties(properties1);
        types.add(testObject);
        JsonObjectType testObject2 = new JsonObjectType("testObject2", "", null, null, null, null, false, false, false);
        testObject2.setBase(testObject);
        types.add(testObject2);
        
        JsonObjectType testObject3 = new JsonObjectType("testObject3", "", null, null, null, null, false, false, true);
        List<JsonProperty> properties2 = new ArrayList<JsonProperty>();
        properties2.add(new JsonProperty("prop1", "", testBoolean, true, true, null));
        properties2.add(new JsonProperty("prop12", "", testString2, true, true, null));
        properties2.add(new JsonProperty("prop13", "", testObject3, true, true, null));
        testObject3.setProperties(properties2);
        testObject3.setBase(testObject2);
        types.add(testObject3);

        testObject2.setProperties(Arrays.<JsonProperty>asList(new JsonProperty("prop14", "", testObject3, true, true, null)));

        JsonSchema schema2 = new JsonSchema("testSchema", "test schema", types);
        assertThat(compareSchema(schema, schema2, true), is(true));
    }
    
    @Test
    public void testSchemaConverters() throws Throwable
    {   
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(Collections.<String>emptySet());
        JsonSchemaLoader loader = new JsonSchemaLoader();
        JsonObjectBuilder object = (JsonObjectBuilder)JsonSerializers.read(new InputStreamReader(Classes.getResource(getClass(), "test-schema2.json"), "UTF-8"), true);
        metaSchema.validate(object, "schema");
        JsonSchema schema = loader.loadSchema(object);
        
        JsonObjectBuilder object1 = Json.object()
            .put("prop1", "yes")
            .put("prop2", "20mb")
            .put("prop3", "1s")
            .put("prop4", "50%")
            .builder();
        JsonObject object2 = Json.object()
            .put("prop1", true)
            .put("prop2", 20 << 20)
            .put("prop3", 1000)
            .put("prop4", 0.5)
            .builder();
        
        schema.validate(object1, "testObject");
        schema.validate(object2, "testObject");

        assertThat(object1.toJson(), is(object2));
    }
    
    @Test
    public void testMetaSchema() throws Throwable
    {
        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(com.exametrika.common.utils.Collections.<String>asSet("test", "type", "typeReference",
            "minMaxCount", "regExp", "stringTypeReference", "property", "object", "objectTypeReference", "schema"));
        
        JsonObjectBuilder metaObject = (JsonObjectBuilder)JsonSerializers.read(new InputStreamReader(Classes.getResource(JsonSchema.class, "schema.schema"), "UTF-8"), true);
        metaSchema.validate(metaObject, "schema");
        
        JsonObjectBuilder object = (JsonObjectBuilder)JsonSerializers.read(new InputStreamReader(Classes.getResource(getClass(), "test-schema.json"), "UTF-8"), true);
        metaSchema.validate(object, "schema");
    }
    
    @Test
    public void testTypeValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonTypeValidator validator = new JsonTypeValidator(com.exametrika.common.utils.Collections.asSet("validator1", "validator2"));
        assertThat(validator.supports(JsonObject.class), is(true));
        assertThat(validator.supports(JsonObjectBuilder.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonArrayBuilder array = new JsonArrayBuilder();
        array.add("validator1");
        array.add("validator2");
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("instanceOf", "object");
        object.put("validators", array);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        object.remove("instanceOf");
        array.add("validator3");
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testTypeReferenceValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        
        JsonTypeReferenceValidator validator = new JsonTypeReferenceValidator(null, 
            com.exametrika.common.utils.Collections.asSet("any", "boolean", "long", "double", "string", "array", "map", "object"));
        assertThat(validator.supports(String.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder type1 = new JsonObjectBuilder();
        type1.put("instanceOf", "object");
        
        JsonObjectBuilder type2 = new JsonObjectBuilder();
        type2.put("instanceOf", "string");
        
        JsonObjectBuilder map = new JsonObjectBuilder();
        map.put("type1", type1);
        map.put("type2", type2);
        
        JsonObjectBuilder root = new JsonObjectBuilder();
        root.put("instanceOf", "object");
        root.put("types", map);
        validator.validate(anyType, "string", new JsonValidationContext(schema, diagnostics, anyType, root));
        validator.validate(anyType, "type1", new JsonValidationContext(schema, diagnostics, anyType, root));
        validator.validate(anyType, "type2", new JsonValidationContext(schema, diagnostics, anyType, root));
        assertThat(diagnostics.isValid(), is(true));
        
        validator.validate(anyType, "type3", new JsonValidationContext(schema, diagnostics, anyType, root));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        
        diagnostics = new JsonDiagnostics();
        
        validator = new JsonTypeReferenceValidator("object", com.exametrika.common.utils.Collections.asSet("object"));
        validator.validate(anyType, "object", new JsonValidationContext(schema, diagnostics, anyType, root));
        validator.validate(anyType, "type1", new JsonValidationContext(schema, diagnostics, anyType, root));
        assertThat(diagnostics.isValid(), is(true));
        
        validator.validate(anyType, "type2", new JsonValidationContext(schema, diagnostics, anyType, root));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testPathReferenceValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonPathReferenceValidator validator = new JsonPathReferenceValidator("path.elements");
        assertThat(validator.supports(String.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder element1 = new JsonObjectBuilder();
        JsonObjectBuilder element2 = new JsonObjectBuilder();
        
        JsonObjectBuilder map = new JsonObjectBuilder();
        map.put("element1", element1);
        map.put("element2", element2);
        
        JsonObjectBuilder root = new JsonObjectBuilder();
        root.update("path.elements", map);
        validator.validate(anyType, "element1", new JsonValidationContext(schema, diagnostics, anyType, root));
        validator.validate(anyType, "element2", new JsonValidationContext(schema, diagnostics, anyType, root));
        assertThat(diagnostics.isValid(), is(true));
        
        validator.validate(anyType, "element3", new JsonValidationContext(schema, diagnostics, anyType, root));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testMinMaxCountValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null,  null, null);
        JsonMinMaxCountValidator validator = new JsonMinMaxCountValidator();
        assertThat(validator.supports(JsonObject.class), is(true));
        assertThat(validator.supports(JsonObjectBuilder.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("minCount", 0l);
        object.put("maxCount", "unbounded");
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        object = new JsonObjectBuilder();
        object.put("minCount", 10l);
        object.put("maxCount", 5);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testRegExpValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonRegExpValidator validator = new JsonRegExpValidator();
        assertThat(validator.supports(String.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        validator.validate(anyType, "[0-9]+", new JsonValidationContext(schema, diagnostics, anyType, ""));
        assertThat(diagnostics.isValid(), is(true));
        
        validator.validate(anyType, "[0-9+", new JsonValidationContext(schema, diagnostics, anyType, ""));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testPropertyValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonPropertyValidator validator = new JsonPropertyValidator();
        assertThat(validator.supports(JsonObject.class), is(true));
        assertThat(validator.supports(JsonObjectBuilder.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("default", 123l);
        object.put("required", false);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        assertThat(diagnostics.isValid(), is(true));
        
        object.put("required", true);
        object.put("allowed", false);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testObjectTypeValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonObjectTypeValidator validator = new JsonObjectTypeValidator();
        assertThat(validator.supports(JsonObject.class), is(true));
        assertThat(validator.supports(JsonObjectBuilder.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("abstract", true);
        object.put("final", true);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        diagnostics = new JsonDiagnostics();
     
        object = new JsonObjectBuilder();
        object.put("enumeration", new JsonArrayBuilder());
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, object));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
        diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder base = new JsonObjectBuilder();
        base.put("instanceOf", "object");
        base.put("final", true);
        
        JsonObjectBuilder map = new JsonObjectBuilder();
        map.put("base", base);
        
        JsonObjectBuilder root = new JsonObjectBuilder();
        root.put("instanceOf", "object");
        root.put("types", map);
        
        object = new JsonObjectBuilder();
        object.put("base", "base");
        object.put("open", true);
        validator.validate(anyType, object, new JsonValidationContext(schema, diagnostics, anyType, root));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
        diagnostics = new JsonDiagnostics();
    }

    @Test
    public void testSchemaValidator()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonSchemaValidator validator = new JsonSchemaValidator();
        assertThat(validator.supports(JsonObject.class), is(true));
        assertThat(validator.supports(JsonObjectBuilder.class), is(true));
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("instanceOf", "object");
        object.put("base", "test2");
        
        JsonObjectBuilder object2 = new JsonObjectBuilder();
        object2.put("instanceOf", "object");
        object2.put("base", "test");
        
        JsonObjectBuilder types = new JsonObjectBuilder();
        types.put("test", object);
        types.put("test2", object2);
        
        JsonObjectBuilder schemaObject = new JsonObjectBuilder();
        schemaObject.put("types", types);
        
        validator.validate(anyType, schemaObject, new JsonValidationContext(schema, diagnostics, anyType, schemaObject));
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }

    @Test
    public void testBooleanConverter()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonBooleanConverter converter = new JsonBooleanConverter();
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        JsonValidationContext context = new JsonValidationContext(schema, diagnostics, anyType, object);
        assertThat((Boolean)converter.convert(anyType, "true", context), is(true));
        assertThat((Boolean)converter.convert(anyType, "yes", context), is(true));
        assertThat((Boolean)converter.convert(anyType, "on", context), is(true));
        assertThat(!(Boolean)converter.convert(anyType, "false", context), is(true));
        assertThat(!(Boolean)converter.convert(anyType, "no", context), is(true));
        assertThat(!(Boolean)converter.convert(anyType, "off", context), is(true));
        
        assertThat(diagnostics.isValid(), is(true));
        
        converter.convert(anyType, "test", context);
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(1));
    }
    
    @Test
    public void testBytesLongConverter()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonByteLongConverter converter = new JsonByteLongConverter();
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        JsonValidationContext context = new JsonValidationContext(schema, diagnostics, anyType, object);
        assertThat((Long)converter.convert(anyType, "1000", context), is(1000l));
        assertThat((Long)converter.convert(anyType, "1000b", context), is(1000l));
        assertThat((Long)converter.convert(anyType, "1Kb", context), is(1l << 10));
        assertThat((Long)converter.convert(anyType, "23mB", context), is(23l << 20));
        assertThat((Long)converter.convert(anyType, "432GB", context), is(432l << 30));
        assertThat((Long)converter.convert(anyType, "500tb", context), is(500l << 40));
        
        assertThat(diagnostics.isValid(), is(true));
        
        converter.convert(anyType, "test", context);
        converter.convert(anyType, "123test", context);
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testPeriodsLongConverter()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonPeriodLongConverter converter = new JsonPeriodLongConverter();
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        JsonValidationContext context = new JsonValidationContext(schema, diagnostics, anyType, object);
        assertThat((Long)converter.convert(anyType, "1000", context), is(1000l));
        assertThat((Long)converter.convert(anyType, "1000ms", context), is(1000l));
        assertThat((Long)converter.convert(anyType, "1s", context), is(1l * 1000));
        assertThat((Long)converter.convert(anyType, "23M", context), is(23l * 60000));
        assertThat((Long)converter.convert(anyType, "432h", context), is(432l * 3600000));
        assertThat((Long)converter.convert(anyType, "1d", context), is(1l * 3600000 * 24));
        
        assertThat(diagnostics.isValid(), is(true));
        
        converter.convert(anyType, "test", context);
        converter.convert(anyType, "123test", context);
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    @Test
    public void testPercentDoubleConverter()
    {
        JsonAnyType anyType = new JsonAnyType("any", "any type", null, null, null, null);
        JsonPercentDoubleConverter converter = new JsonPercentDoubleConverter();
        
        JsonSchema schema = new JsonSchema("", "", Collections.<JsonType>emptyList());
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        
        JsonObjectBuilder object = new JsonObjectBuilder();
        JsonValidationContext context = new JsonValidationContext(schema, diagnostics, anyType, object);
        assertThat((Double)converter.convert(anyType, "1000.123", context), is(1000.123d));
        assertThat((Double)converter.convert(anyType, "50.3%", context), is(50.3d/100));
        
        assertThat(diagnostics.isValid(), is(true));
        
        converter.convert(anyType, "test", context);
        converter.convert(anyType, "123test", context);
        
        assertThat(diagnostics.isValid(), is(false));
        assertThat(diagnostics.getErrors().size(), is(2));
    }
    
    private boolean compareSchema(Object o1, Object o2, boolean topLevel)
    {
        if (o1 == null && o1 == o2)
            return true;
        if (o1 == null || o2 == null)
            return false;
        if (o1 instanceof JsonSchema)
        {
            JsonSchema s1 = (JsonSchema)o1;
            JsonSchema s2 = (JsonSchema)o2;
            if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()))
                return false;
            
            if (s1.getTypes().size() != s2.getTypes().size())
                return false;
            for (int i = 0; i < s1.getTypes().size(); i++)
            {
                JsonType t1 = s1.getTypes().get(i);
                JsonType t2 = s2.findType(t1.getName());
                if (!compareSchema(t1, t2, true))
                    return false;
            }
            return true;
        }
        else if (o1 instanceof JsonAnyType)
        {
            JsonAnyType s1 = (JsonAnyType)o1;
            JsonAnyType s2 = (JsonAnyType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()))
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonArrayType)
        {
            JsonArrayType s1 = (JsonArrayType)o1;
            JsonArrayType s2 = (JsonArrayType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.getMinCount() != s1.getMinCount() || s1.getMaxCount() != s1.getMaxCount() ||
                    s1.isAllowDuplicates() != s2.isAllowDuplicates() ||
                    s1.isAllowNulls() != s2.isAllowNulls())
                    return false;
                
                if (!compareSchema(s1.getElementType(), s2.getElementType(), false))
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonBooleanType)
        {
            JsonBooleanType s1 = (JsonBooleanType)o1;
            JsonBooleanType s2 = (JsonBooleanType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()))
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonCompoundType)
        {
            JsonCompoundType s1 = (JsonCompoundType)o1;
            JsonCompoundType s2 = (JsonCompoundType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()))
                    return false;
                
                if (s1.getTypes().size() != s2.getTypes().size())
                    return false;
                for (int i = 0; i < s1.getTypes().size(); i++)
                {
                    if (!compareSchema(s1.getTypes().get(i), s2.getTypes().get(i), false))
                        return false;
                }
                return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonDoubleType)
        {
            JsonDoubleType s1 = (JsonDoubleType)o1;
            JsonDoubleType s2 = (JsonDoubleType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.getMin() != s2.getMin() || s1.getMax() != s2.getMax() || s1.isMinExclusive() != s2.isMinExclusive() ||
                    s1.isMaxExclusive() != s2.isMaxExclusive())
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonLongType)
        {
            JsonLongType s1 = (JsonLongType)o1;
            JsonLongType s2 = (JsonLongType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.getMin() != s2.getMin() || s1.getMax() != s2.getMax() || s1.isMinExclusive() != s2.isMinExclusive() ||
                    s1.isMaxExclusive() != s2.isMaxExclusive())
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonMapType)
        {
            JsonMapType s1 = (JsonMapType)o1;
            JsonMapType s2 = (JsonMapType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.getMinCount() != s1.getMinCount() || s1.getMaxCount() != s1.getMaxCount() ||
                    s1.isAllowNulls() != s2.isAllowNulls())
                    return false;
                
                if (!compareSchema(s1.getKeyType(), s2.getKeyType(), false))
                    return false;
                
                if (!compareSchema(s1.getValueType(), s2.getValueType(), false))
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonObjectType)
        {
            JsonObjectType s1 = (JsonObjectType)o1;
            JsonObjectType s2 = (JsonObjectType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.isOpen() != s1.isOpen() || s1.isAbstract() != s1.isAbstract() ||
                    s1.isFinal() != s2.isFinal())
                    return false;
                
                if (!compareSchema(s1.getBase(), s2.getBase(), false))
                    return false;
                
                if (s1.getProperties().size() != s2.getProperties().size())
                    return false;
                for (int i = 0; i < s1.getProperties().size(); i++)
                {
                    if (!compareSchema(s1.getProperties().get(i), s2.getProperties().get(i), false))
                        return false;
                }
                return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else if (o1 instanceof JsonProperty)
        {
            JsonProperty s1 = (JsonProperty)o1;
            JsonProperty s2 = (JsonProperty)o2;
            
            if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                !Objects.equals(s1.getDefaultValue(), s2.getDefaultValue()) ||
                s1.isRequired() != s1.isRequired() || s1.isAllowed() != s1.isAllowed())
                return false;
            
            if (!compareSchema(s1.getType(), s2.getType(), false))
                return false;
            else
                return true;
        }
        else if (o1 instanceof JsonStringType)
        {
            JsonStringType s1 = (JsonStringType)o1;
            JsonStringType s2 = (JsonStringType)o2;
            
            if (topLevel || s1.getName().isEmpty())
            {
                if (!s1.getName().equals(s2.getName()) || !s1.getDescription().equals(s2.getDescription()) ||
                    !Objects.equals(s1.getEnumeration(), s2.getEnumeration()) || !Objects.equals(s1.getAnnotation(), s2.getAnnotation()) ||
                    !Objects.equals(s1.getValidators(), s2.getValidators()) ||
                    s1.getMinCount() != s1.getMinCount() || s1.getMaxCount() != s1.getMaxCount() ||
                            !Objects.equals(s1.getPattern(), s2.getPattern()))
                    return false;
                else
                    return true;
            }
            else 
                return s1.getName().equals(s2.getName());
        }
        else
            return Assert.error();
    }
    
    private static class TestValidator1 implements IJsonValidator
    {
        private final List<Class> classes;
        private final List<Object> instances;
        private final String error;

        public TestValidator1(List<Class> classes, List<Object> instances, String error)
        {
            this.classes = classes;
            this.instances = instances;
            this.error = error;
        }
        
        @Override
        public boolean supports(Class clazz)
        {
            return classes.indexOf(clazz) != -1;
        }

        @Override
        public void validate(JsonType type, Object instance, IJsonValidationContext context)
        {
            if (instances.indexOf(instance) == -1)
                context.getDiagnostics().addError(messages.testError(context.getDiagnostics().getPath(), error));
        }
    }
    
    private static class TestValidator2 implements IJsonValidator
    {
        @Override
        public boolean supports(Class clazz)
        {
            return true;
        }

        @Override
        public void validate(JsonType type, Object instance, IJsonValidationContext context)
        {
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestValidator2))
                return false;
            
            return true;
        }
        
        @Override
        public int hashCode()
        {
            return getClass().hashCode();
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Test error ''{1}''.")
        ILocalizedMessage testError(String path, String error);
    }
}