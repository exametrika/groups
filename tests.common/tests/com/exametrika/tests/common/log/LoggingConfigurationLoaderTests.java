/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.log;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;
import com.exametrika.common.log.config.FileAppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.log.config.TextAppenderConfiguration;
import com.exametrika.common.log.impl.IAppender;
import com.exametrika.common.log.impl.LogContext;
import com.exametrika.common.log.impl.TextAppender;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link LoggingConfigurationLoaderTests} are tests for loader of logging configuration.
 * 
 * @author Medvedev-A
 */
public class LoggingConfigurationLoaderTests
{
    public static class TestLoggingConfigurationExtension implements IConfigurationLoaderExtension
    {
        @Override
        public Parameters getParameters()
        {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.logging", new Pair("classpath:" + Classes.getResourcePath(getClass()) + "/logging-extension.json", false));
            parameters.typeLoaders.put("TestAppender", new TestAppenderProcessor());
            return parameters;
        }
    }
    
    @Test
    public void testConfigurationLoad()
    {
        System.setProperty("com.exametrika.home", "/test");
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        LoggingConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(LoggingConfiguration.SCHEMA);
        
        String logPath = "/test/logs/instrument.log"; 
        LoggerConfiguration rootLogger = new LoggerConfiguration("", LogLevel.ERROR, Collections.<String>emptyList(),
            Arrays.<String>asList("filter1", "filter2"), false);
        LoggingConfiguration loggerConfiguration = new LoggingConfiguration(RuntimeMode.PRODUCTION, Arrays.<AppenderConfiguration>asList(
            new FileAppenderConfiguration("file1", null, "test", new File(logPath), Collections.<String>emptyList()),
            new ConsoleAppenderConfiguration("console", LogLevel.DEBUG, "test", Arrays.<String>asList("filter1", "filter2"),
                Target.ERROR, true), 
            new TestAppenderConfiguration("test", LogLevel.DEBUG, "test", Collections.<String>emptyList())),  
            rootLogger, Arrays.asList(
            new LoggerConfiguration("test1", LogLevel.WARNING, Arrays.<String>asList("file1", "console"), Collections.<String>emptyList(), false),
            new LoggerConfiguration("test2", LogLevel.INFO, Collections.<String>emptyList(), Collections.<String>emptyList(), false),
            new LoggerConfiguration("test3", LogLevel.DEBUG, Collections.<String>emptyList(), Collections.<String>emptyList(), false),
            new LoggerConfiguration("test4", LogLevel.TRACE, Arrays.<String>asList("console"), Collections.<String>emptyList(), false),
            new LoggerConfiguration("test5", LogLevel.TRACE, Arrays.<String>asList("test"), Arrays.<String>asList("logger == 'Test'"),
                true)
                ));
        
        assertThat(configuration, is(loggerConfiguration));
    }
    
    private static String getResourcePath()
    {
        String className = LoggingConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
    
    public static final class TestAppenderConfiguration extends TextAppenderConfiguration
    {
        public TestAppenderConfiguration(String name, LogLevel level, String template, List<String> filters)
        {
            super(name, level, template, filters);
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestAppenderConfiguration))
                return false;
            
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public IAppender createAppender(CompileContext compileContext, LogContext context, 
            Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
        {
            return new TestAppender(getLevel(), getTemplate(), compileContext, context, runtimeContext, templateRegistry);
        }
    }
    
    private static class TestAppender extends TextAppender
    {
        public TestAppender(LogLevel level, String template, CompileContext compileContext, 
            LogContext context, Map<String, Object> runtimeContext, ITemplateRegistry templateRegistry)
        {
            super(level, template, Collections.<String>emptyList(), compileContext, context, runtimeContext, templateRegistry);
        }
        
        @Override
        public void flush()
        {
        }

        @Override
        public void start()
        {
        }

        @Override
        public void stop()
        {
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestAppender))
                return false;
            
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public String toString()
        {
            return super.toString() + " test";
        }
        
        @Override
        protected void doAppend(String message)
        {
        }
    }
    private static class TestAppenderProcessor implements IExtensionLoader
    {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context)
        {
            JsonObject element = (JsonObject)object;
            LogLevel level = loadLogLevel((String)element.get("level"));
            
            String pattern = element.get("template");
            return new TestAppenderConfiguration(name, level, pattern, Collections.<String>emptyList());
        }
        
        private LogLevel loadLogLevel(String level)
        {
            if (level.equals("trace"))
                return LogLevel.TRACE;
            else if (level.equals("debug"))
                return LogLevel.DEBUG;
            else if (level.equals("info"))
                return LogLevel.INFO;
            else if (level.equals("warning"))
                return LogLevel.WARNING;
            else if (level.equals("error"))
                return LogLevel.ERROR;
            else
                throw new InvalidConfigurationException();
        }

        @Override
        public void setExtensionLoader(IExtensionLoader extensionProcessor)
        {
        }
    }
}
