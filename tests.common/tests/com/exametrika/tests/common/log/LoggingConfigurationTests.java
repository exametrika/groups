/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.log;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;


/**
 * The {@link LoggingConfigurationTests} are tests for {@link LoggingConfiguration} class.
 * 
 * @see LoggingConfiguration
 * @author Medvedev_A
 */
public class LoggingConfigurationTests
{
    @Test
    public void testConfiguration() throws Throwable
    {
        AppenderConfiguration appender = new ConsoleAppenderConfiguration("appender", LogLevel.DEBUG, "test", Collections.<String>emptyList(),
            Target.OUTPUT, false);
        LoggerConfiguration root = new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), false);
        LoggerConfiguration info1 = new LoggerConfiguration("test", LogLevel.DEBUG, Arrays.<String>asList("appender"), 
            Collections.<String>emptyList(), false);
        LoggerConfiguration info2 = new LoggerConfiguration("test.test2", LogLevel.TRACE, Collections.<String>emptyList(), 
            Collections.<String>emptyList(), false);
        LoggingConfiguration configuration = new LoggingConfiguration(RuntimeMode.DEVELOPMENT, 
            Arrays.<AppenderConfiguration>asList(appender), root, Arrays.asList(info1, info2));
        
        assertThat(configuration.getLogger(""), is(root));
        assertThat(configuration.getLogger("test"), is(info1));
        
        LoggerConfiguration info3 = configuration.getLogger("test.test2");
        assertThat(info3.getName(), is(info2.getName()));
        assertThat(info3.getLevel(), is(info2.getLevel()));
        assertThat(info3.getAppenders(), is(info1.getAppenders()));
        
        LoggerConfiguration info4 = configuration.getLogger("test.test2.test3");
        assertThat(info4.getName(), is("test.test2.test3"));
        assertThat(info4.getLevel(), is(info2.getLevel()));
        assertThat(info4.getAppenders(), is(info1.getAppenders()));
        
        LoggerConfiguration info5 = configuration.getLogger("test1");
        assertThat(info5.getName(), is("test1"));
        assertThat(info5.getLevel(), is(root.getLevel()));
        assertThat(info5.getAppenders(), is(root.getAppenders()));
    }
}
