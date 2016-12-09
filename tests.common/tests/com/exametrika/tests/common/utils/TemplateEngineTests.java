/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.TemplateEngine;


/**
 * The {@link TemplateEngineTests} are tests for {@link TemplateEngine}.
 * 
 * @see TemplateEngine
 * @author Medvedev-A
 */
public class TemplateEngineTests
{
    private TemplateEngine templateEngine;
    private Reader templateReader;
    private String originalTemplate;
    
    @Before
    public void setUp() throws IOException
    {
        templateEngine = new TemplateEngine();

        String className = getClass().getName();
        String testResourceName = className.substring(0, className.lastIndexOf('.') + 1).replace('.', '/') + "TestTemplate.txt";
        templateReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(testResourceName));
        
        BufferedReader originalReader = null;
        try
        {
            testResourceName = className.substring(0, className.lastIndexOf('.') + 1).replace('.', '/') + "OriginalTemplate.txt";
            originalReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(testResourceName)));
            originalTemplate = getText(originalReader);
        }
        finally
        {
            IOs.close(originalReader);
        }
    }

    @After
    public void tearDown() throws IOException
    {
        IOs.close(templateReader);
    }
    
    @Test
    public void testTemplateEngine() throws Exception
    {
        String template = templateEngine.createTemplate(templateReader);
        assertThat(template, is(originalTemplate));
    }
    
    private String getText(BufferedReader reader) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            String line = reader.readLine();
            if (line == null)
                break;
            
            builder.append(line);
            builder.append('\n');
        }
        return builder.toString();
    }
}
