/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.json;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonPath;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link JsonPathTests} are tests for {@link JsonPath}.
 * 
 * @see JsonPath
 * @author Medvedev-A
 */
public class JsonPathTests
{
    @Test
    public void testPath() throws Throwable
    {
        JsonArrayBuilder list = new JsonArrayBuilder();
        
        
        JsonObjectBuilder map = new JsonObjectBuilder();
        JsonObjectBuilder map2 = new JsonObjectBuilder();
        map2.put("key1", 123);
        map2.put("key2", "test string");
        list.add(map2);
        map.put("list", list);
        map.put("complex [property \"\" name \"\"]", map2);
        map.put("simple", map2);
        JsonArray listValue = list.toJson();
        final JsonObject mapValue = map.toJson();
        
        assertThat(new JsonPath("?").get(null), nullValue());
        assertThat(new JsonPath("").get(mapValue) == mapValue, is(true));
        assertThat(new JsonPath("list").get(mapValue).equals(listValue), is(true));
        assertThat(new JsonPath(".list").get(mapValue).equals(listValue), is(true));
        assertThat((Long)new JsonPath("[\"complex [property \"\" name \"\"]\"][\"key1\"]").get(mapValue), is(123l));
        assertThat((Long)new JsonPath("simple.key1").get(mapValue), is(123l));
        assertThat((String)new JsonPath("simple.key2").get(mapValue), is("test string"));
        assertThat((String)new JsonPath("simple[\"key2\"]").get(mapValue), is("test string"));
        assertThat(new JsonPath("simple.absent?").get(mapValue), nullValue());
        assertThat(new JsonPath("absent?.key2").get(mapValue), nullValue());
        assertThat((String)new JsonPath("list[0].key2").get(mapValue), is("test string"));
        assertThat(new JsonPath("list[100]?.key2").get(mapValue), nullValue());
        
        assertThat(mapValue.get("list").equals(listValue), is(true));
        assertThat(mapValue.select("list").equals(listValue), is(true));
        
        assertThat((String)listValue.select("[0].key2"), is("test string"));
        
        JsonObjectBuilder mapBuilder = new JsonObjectBuilder();
        mapBuilder.update("simple.key1", 123L);
        mapBuilder.update("simple.key2", "test string");
        mapBuilder.update("list[0].key1", 123);
        mapBuilder.update("list[0].key2", "test string");
        mapBuilder.update("[\"complex [property \"\" name \"\"]\"][\"key1\"]", 123);
        mapBuilder.update("[\"complex [property \"\" name \"\"]\"].key2", "test string");
        
        assertThat(mapBuilder.toJson(), is(mapValue));
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("").get(null);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("absent.key2").get(mapValue);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("simple.absent").get(mapValue);
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("list[100].key2").get(mapValue);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("[0]").get(mapValue);
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                new JsonPath("list.key1").get(mapValue);
            }
        });
   }
}