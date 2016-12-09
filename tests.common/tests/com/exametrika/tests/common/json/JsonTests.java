/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.json;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;

import org.junit.Test;

import com.exametrika.common.expression.Expressions;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonReader;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.json.JsonWriter;
import com.exametrika.common.utils.Classes;


/**
 * The {@link JsonTests} are tests for JSON elements.
 * 
 * @see JsonWriter
 * @see JsonReader
 * @see JsonSerializers
 * @author Medvedev-A
 */
public class JsonTests
{
    @Test
    public void testReader() throws Throwable
    {
        JsonObject object = (JsonObject)JsonSerializers.read(new InputStreamReader(Classes.getResource(getClass(), "test.json"), "UTF-8"), false);
        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.put("key1", 100.123d);
        builder.put("key2", true);
        builder.put("key3", 100);
        builder.put("key4", "\"Hello world!!!\"\t\\\u8080");
        builder.put("key5", null);
        JsonArrayBuilder builder2 = new JsonArrayBuilder();
        builder.put("key6", builder2);
        
        builder2.add(100);
        builder2.add(200);
        
        JsonObjectBuilder builder3 = new JsonObjectBuilder();
        builder2.add(builder3);
        
        builder3.put("key1", 100.123);
        builder3.put("key2", true);
        
        JsonObjectBuilder builder4 = new JsonObjectBuilder();
        builder2.add(builder4);
        
        builder4.put("key1", -1.001E-13);
        builder4.put("key2", true);
        
        JsonArrayBuilder builder5 = new JsonArrayBuilder();
        builder.put("key7", builder5);
        
        builder5.add("aa");
        builder5.add("bb");
        builder5.add("cc");
        
        builder.put("key8", "value8");
        
        JsonObjectBuilder builder6 = new JsonObjectBuilder();
        builder.put("key9", builder6);
        
        builder6.put("key10", "value9");
        
        JsonArrayBuilder builder7 = new JsonArrayBuilder();
        builder.put("key11", builder7);
        
        builder7.add(1000);
        builder7.add(1000);
        builder7.add(123);
        
        JsonArrayBuilder builder8 = new JsonArrayBuilder();
        builder.put("key12", builder8);
        
        builder8.add("a");
        builder8.add("b");
        builder8.add("c");
        
        JsonObjectBuilder builder9 = new JsonObjectBuilder();
        builder.put("key13", builder9);
        
        builder9.put("a", "1");
        builder9.put("b", "2");
        builder9.put("c", null);
        
        builder.put("key14", " Hello world с:\\test\\test.json\n\t\t\tпривет мир.");
        builder.put("key15", "Hello world.привет мир!!!");
        builder.put("key16", "Hello world.привет мир!!!");
        builder.put("key17", 0xABCDEF123456789l);
        builder.put("key18", new JsonObjectBuilder());
        builder.put("key19", new JsonArrayBuilder());
        builder.put("key20", "");
        
        assertThat(object, is(builder.toJson()));
    }
    
    @Test
    public void testWriter() throws Throwable
    {
        File file = File.createTempFile("writer", "tmp");
        Writer writer = new FileWriter(file);
        JsonWriter jsonWriter = new JsonWriter(writer, 4);
        
        jsonWriter.startObject();
        jsonWriter.key("key1");
        jsonWriter.value(100.123d);
        jsonWriter.key("key2");
        jsonWriter.value(true);
        jsonWriter.key("key3");
        jsonWriter.value(100l);
        jsonWriter.key("key4");
        jsonWriter.value("\"Hello world!!!\"\t\\\u0080");
        jsonWriter.key("key5");
        jsonWriter.value(null);
        jsonWriter.key("key6");
        
        jsonWriter.startArray();
        jsonWriter.value(100l);
        jsonWriter.value(200l);
        jsonWriter.startObject();
        jsonWriter.key("key1");
        jsonWriter.value(100.123d);
        jsonWriter.key("key2");
        jsonWriter.value(true);
        jsonWriter.endObject();
        
        jsonWriter.startObject();
        jsonWriter.key("key1");
        
        jsonWriter.value(-100.1235);
        
        jsonWriter.key("key2");
        jsonWriter.value(true);
        jsonWriter.endObject();
        
        jsonWriter.endArray();
        
        jsonWriter.key("key7");
        jsonWriter.startArray();
        jsonWriter.value("aa");
        jsonWriter.value("bb");
        jsonWriter.value("cc");
        jsonWriter.endArray();
        
        jsonWriter.endObject();
        
        jsonWriter.close();
        writer.close();
        
        JsonObject object = (JsonObject)JsonSerializers.read(new FileReader(file), false);
        
        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.put("key1", 100.123d);
        builder.put("key2", true);
        builder.put("key3", 100);
        builder.put("key4", "\"Hello world!!!\"\t\\\u0080");
        builder.put("key5", null);
        JsonArrayBuilder builder2 = new JsonArrayBuilder();
        builder.put("key6", builder2);
        
        builder2.add(100);
        builder2.add(200);
        
        JsonObjectBuilder builder3 = new JsonObjectBuilder();
        builder2.add(builder3);
        
        builder3.put("key1", 100.123);
        builder3.put("key2", true);
        
        JsonObjectBuilder builder4 = new JsonObjectBuilder();
        builder2.add(builder4);
        
        builder4.put("key1", -100.124);
        builder4.put("key2", true);
        
        JsonArrayBuilder builder5 = new JsonArrayBuilder();
        builder.put("key7", builder5);
        
        builder5.add("aa");
        builder5.add("bb");
        builder5.add("cc");

        assertThat(object, is(builder.toJson()));
        
        file.delete();
    }
    
    @Test
    public void testSerialization() throws Throwable
    {
        JsonObject object = (JsonObject)JsonSerializers.read(new InputStreamReader(Classes.getResource(getClass(), "test.json"), "UTF-8"), false);
        
        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        
        JsonSerializers.serialize(serialization, object);
        
        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        
        JsonObject object2 = (JsonObject)JsonSerializers.deserialize(deserialization);
        
        assertThat(object2, is(object));
    }
    
    @Test
    public void testDiff()
    {
        JsonObject value1 = Json.object()
            .put("key", "value")
            .putObject("key1")
                .put("key", "value")
            .end()
            .putObject("key2")
                .put("key", "value")
            .end()
            .putArray("key4")
                .add("value")
                .add("value1")
                .add("value2")
            .end()
            .putArray("key5")
                .add("value1")
            .end()
            .putArray("key6")
                .add(10)
            .end()
            .putArray("key7")
                .addObject()
                    .put("key10", "value10")
                    .put("key11", 10l)
                    .put("key12", 10.1234d)
                .end()
            .end().toObject();
        
        JsonObject value2 = Json.object()
            .put("key", "value")
            .put("key2", 10)
            .put("key3", "value3")
            .putArray("key4")
                .add("value")
                .add("value1")
            .end()
            .putArray("key5")
                .add("value1")
                .add("value2")
            .end()
            .putArray("key6")
                .add("value")
            .end()
            .putArray("key7")
                .addObject()
                    .put("key10", "value1")
                    .put("key11", 20l)
                    .put("key12", 20.2468d)
                .end()
            .end().toObject();
    
        JsonObject diffFull = Json.object()
                .putObject("+key1")
                    .put("key", "value")
                .end()
                .putObject("+key2")
                    .put("key", "value")
                .end()
                .put("-key2", 10)
                .put("-key3", "value3")
                .putObject("~key4")
                    .put("+[2]", "value2")
                .end()
                .putObject("~key5")
                    .put("-[1]", "value2")
                .end()
                .putObject("~key6")
                    .put("+[0]", 10)
                    .put("-[0]", "value")
                .end()
                .putObject("~key7")
                    .putObject("~[0]")
                        .put("+key10", "value10")
                        .put("-key10", "value1")
                        .put("~key11", "-10 (10 - 20)")
                        .put("~key12", "-10.123 (10.123 - 20.247)")
                    .end()
                .end()
                .toObject();
        JsonObject diffShort = Json.object()
                .put("+key1", "")
                .put("+key2", "")
                .put("-key2", "")
                .put("-key3", "")
                .putObject("~key4")
                    .put("+[2]", "")
                .end()
                .putObject("~key5")
                    .put("-[1]", "")
                .end()
                .putObject("~key6")
                    .put("+[0]", "")
                    .put("-[0]", "")
                .end()
                .putObject("~key7")
                    .putObject("~[0]")
                        .put("+key10", "")
                        .put("-key10", "")
                        .put("~key11", -10l)
                        .put("~key12", -10.1234d)
                    .end()
                .end()
                .toObject();
        
        JsonObject diff = new JsonDiff().diff(value1, value2);
        assertThat(diff, is(diffFull));
        
        JsonObject diff2 = new JsonDiff(false).diff(value1, value2);
        assertThat(diff2, is(diffShort));
    }
    
    @Test
    public void testExpression()
    {
        JsonObjectBuilder builder = Json.object()
            .putArray("a")
                .add("value1")
            .end().builder();
        JsonObject object = builder.toJson();
        
        assertThat((String)Expressions.evaluate("a[0]", object, null), is("value1"));
        Expressions.evaluate("a[0]='value2'", builder, null);
        assertThat((String)Expressions.evaluate("a[0]", builder, null), is("value2"));
    }
    
    @Test
    public void testTruncate()
    {
        JsonObject json = Json.object()
            .put("key1", "value1")
            .put("key2", 2l)
            .put("key3", 3d)
            .put("key4", true)
            .put("key5", null)
            .putArray("key6")
                .add("very long value")
                .add("very long value2")
            .end()
            .toObject();
        
        assertThat(JsonUtils.truncate(json, 0, false), nullValue());
        assertThat((JsonObject)JsonUtils.truncate(json, 3, false), is(Json.object().toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 4, true), is(Json.object().put("...", "...").toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 5, false), is(Json.object().put("key1", "v").toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 8, true), is(Json.object().put("key1", "v...").put("...", "...").toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 10, false), is(Json.object().put("key1", "value1").toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 22, false), is(Json.object().put("key1", "value1")
            .put("key2", 2l).toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 34, false), is(Json.object().put("key1", "value1")
            .put("key2", 2l).put("key3", 3d).toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 39, false), is(Json.object().put("key1", "value1")
            .put("key2", 2l).put("key3", 3d).put("key4", true).toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 44, false), is(Json.object().put("key1", "value1")
            .put("key2", 2l).put("key3", 3d).put("key4", true).put("key5", null).toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 49, false), is(Json.object().put("key1", "value1")
            .put("key2", 2l).put("key3", 3d).put("key4", true).put("key5", null)
            .putArray("key6")
                .add("v")
            .end().toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 52, true), is(Json.object().put("key1", "value1")
            .put("key2", 2l).put("key3", 3d).put("key4", true).put("key5", null)
            .putArray("key6")
                .add("v...")
                .add("...")
            .end().toObject()));
        assertThat((JsonObject)JsonUtils.truncate(json, 100, true), is(json));
    }
}