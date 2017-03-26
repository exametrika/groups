/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.shell.IShellParameterValidator;
import com.exametrika.common.shell.impl.ShellCommand;
import com.exametrika.common.shell.impl.converters.FileConverter;
import com.exametrika.common.shell.impl.converters.IntegerConverter;
import com.exametrika.common.shell.impl.converters.URIConverter;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link CommandLineParserTests} are tests for {@link ShellCommand}.
 * 
 * @see ShellCommand
 * @author Medvedev-A
 */
public class CommandLineParserTests
{
    @Test
    public void testParser() throws Throwable
    {
        final Map<String, Object> params = new HashMap<String, Object>();

        final ShellCommand parser = new ShellCommand();
        parser.defineParameter("key1", new String[]{"-n1", "--name1"}, "-n1, --name1", "param1\ndescription", true);
        parser.defineParameter("key2", new String[]{"-n2"}, "-n2", "param2\ndescription", true, false, true, null, null);
        parser.defineParameter("key3", new String[]{"-n3"}, "-n3", "param3\ndescription", false, true, true,
            new FileConverter(), new File("/home"));
        parser.defineParameter("key4", new String[]{"-n4"}, "-n4", "param4\ndescription", false, true, true, 
            new URIConverter(), "http://localhost");
        parser.defineParameter("key5", new String[]{"-n5"}, "-n5", "param5\ndescription", false, true, false, 
            new IntegerConverter(), 123);
        parser.defineParameter("key6", new String[]{"-n6", "--name6"}, "-n6, --name6", "param6\ndescription", false);
        
        parser.defineUnnamedParameter("key10", "unnamed_param", "unnamed_param\ndescription", true, true,  
            null, null);
        
        parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n2", "--name1",
            "hello", "-n5", "12"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("ftp://localhost")), new Entry("key5", Arrays.asList(11, 12)),
            new Entry("key10", "hello")})));
        
        // Unrecognized option
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n2", "--name1",
                    "hello", "-n5", "12", "-qq"}, params);
            }
        });
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n2", "--name1",
                    "hello", "-n5", "12", "-qq1", "-qq2"}, params);
            }
        });
        // Duplicate option
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n1", "-n2", "--name1",
                    "hello", "-n5", "12"}, params);
            }
        });
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n1", "-n2",
                    "hello", "-n5", "12", "hello2"}, params);
            }
        });
        // Argument not found
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "-n5", "12", "-n3", "/", "-n1", "-n2", "hello"}, params);
            }
        });
        // Required option not found
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n2",
                    "hello", "-n5", "12"}, params);
            }
        });
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n5", "11", "-n4", "\"ftp://localhost\"", "-n3", "/", "-n2", "-n1",
                    "-n5", "12"}, params);
            }
        });

        parser.parse(new String[]{"-n3", "/", "-n2", "--name1", "hello"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("http://localhost")), new Entry("key5", Arrays.asList(123)),
            new Entry("key10", "hello")})));
        
        parser.defineUnnamedParameter("key10", "unnamed_param", "unnamed_param\ndescription", false, true,
            new URIConverter(), new URI("ftp://hello"));
        
        parser.parse(new String[]{"-n3", "/", "-n2", "--name1"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("http://localhost")), new Entry("key5", Arrays.asList(123)),
            new Entry("key10", new URI("ftp://hello"))})));
        
        parser.defineUnnamedParameter("key10", "unnamed_param", "unnamed_param\ndescription", true, false,
            new URIConverter(), null);
        
        parser.parse(new String[]{"ftp://hello2", "-n3", "/", "-n2", "--name1", "ftp://hello1"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("http://localhost")), new Entry("key5", Arrays.asList(123)),
            new Entry("key10", Arrays.asList(new URI("ftp://hello2"), new URI("ftp://hello1")))})));
        
        parser.defineUnnamedParameter("key10", "unnamed_param", "unnamed_param\ndescription", false, false, 
            new URIConverter(), "ftp://hello");
        
        parser.parse(new String[]{"ftp://hello2", "-n3", "/", "-n2", "--name1", "ftp://hello1"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("http://localhost")), new Entry("key5", Arrays.asList(123)),
            new Entry("key10", Arrays.asList(new URI("ftp://hello2"), new URI("ftp://hello1")))})));
        
        parser.parse(new String[]{"-n3", "/", "-n2", "--name1"}, params);
        assertThat(params, is(asMap(new Entry[]{new Entry("key1", null), new Entry("key2", null),
            new Entry("key3", new File("/")), new Entry("key4", new URI("http://localhost")), new Entry("key5", Arrays.asList(123)),
            new Entry("key10", Arrays.asList(new URI("ftp://hello")))})));
        
        parser.setValidator(new IShellParameterValidator()
        {
            @Override
            public void validate(Map<String, Object> parameters)
            {
                if (parameters.containsKey("key1"))
                    throw new InvalidArgumentException();
            }
        });
        
        new Expected(InvalidArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                parser.parse(new String[]{"-n3", "/", "-n2", "--name1"}, params);
            }
        });
    }
    
    private static class Entry
    {
        String key;
        Object value;
        public Entry(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }
    }
    
    private static Map<String, Object> asMap(Entry[] entries)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Entry entry : entries)
            map.put(entry.key, entry.value);
        
        return map;
    }
}
