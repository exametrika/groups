/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.utils.PropertiesReader;


/**
 * The {@link PropertiesReaderTests} are tests for {@link PropertiesReader}.
 * 
 * @see PropertiesReader
 * @author Medvedev-A
 */
public class PropertiesReaderTests
{
    private PropertiesReader propertiesReader;
    
    @Before
    public void setUp()
    {
        propertiesReader = new PropertiesReader();
    }

    @Test
    public void testReader() throws Exception
    {
        String className = getClass().getName();
        String testResourceName = className.substring(0, className.lastIndexOf('.') + 1).replace('.', '/') + "Test.properties";
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(testResourceName));
        HashMap<String, String> properties = propertiesReader.read(reader);
        
        assertThat(properties.get("property1"), is("property value"));
        assertThat(properties.get("property2"), is("multiline property value"));
        assertThat(properties.get("свойство3"), is("значение свойства3"));
    }
}
