/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;
import com.exametrika.common.log.config.FileAppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.utils.Assert;



/**
 * The {@link LoggingConfigurationLoader} is a configuration loader for logging configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LoggingConfigurationLoader extends AbstractElementLoader
{
    @Override
    public void loadElement(JsonObject element, ILoadContext context)
    {
        LoggingLoadContext loggingContext = context.get(LoggingConfiguration.SCHEMA);

        JsonObject appenders = element.get("appenders", null);
        if (appenders != null)
        {
            for (Map.Entry<String, Object> entry : appenders)
                loggingContext.addAppender(loadAppender(entry.getKey(), (JsonObject)entry.getValue(), context));
        }
        
        JsonObject root = element.get("root", null);
        if (root != null)
            loggingContext.setRootLogger(loadLogger("", root));
        
        JsonObject loggers = element.get("loggers", null);
        if (loggers != null)
        {
            for (Map.Entry<String, Object> entry : loggers)
                loggingContext.addLogger(loadLogger(entry.getKey(), (JsonObject)entry.getValue()));
        }
    }
    
    private AppenderConfiguration loadAppender(String name, JsonObject element, ILoadContext loadContext)
    {
        LogLevel level = loadLogLevel((String)element.get("level", null));
        String template = element.get("template");
        
        JsonArray array = element.get("filters", null);
        List<String> filters = new ArrayList<String>();
        if (array != null)
        {
            for (Object child : array)
                filters.add((String)child);
        }
        
        String type = getType(element);
        if (type.equals("ConsoleAppender"))
        {
            String target = element.get("target");
            boolean colorize = element.get("colorize");
            return new ConsoleAppenderConfiguration(name, level, template, filters, 
                target.equals("output") ? Target.OUTPUT : Target.ERROR, colorize);
        }
        else if (type.equals("FileAppender"))
        {
            String path = element.get("path");
            return new FileAppenderConfiguration(name, level, template, new File(path), filters);
        }
        else
            return load(name, type, element, loadContext); 
    }

    private LoggerConfiguration loadLogger(String name, JsonObject element)
    {
        LogLevel level = loadLogLevel((String)element.get("level"));
        boolean recordStackTrace = element.get("recordStackTrace");
        
        JsonArray array = element.get("appenders", null);
        List<String> appenders = new ArrayList<String>();
        if (array != null)
        {
            for (Object child : array)
                appenders.add((String)child);
        }
        
        array = element.get("filters", null);
        List<String> filters = new ArrayList<String>();
        if (array != null)
        {
            for (Object child : array)
                filters.add((String)child);
        }
        
        return new LoggerConfiguration(name, level, appenders, filters, recordStackTrace);
    }

    private LogLevel loadLogLevel(String level)
    {
        if (level == null)
            return null;
        else if (level.equals("all"))
            return LogLevel.ALL;
        else if (level.equals("trace"))
            return LogLevel.TRACE;
        else if (level.equals("debug"))
            return LogLevel.DEBUG;
        else if (level.equals("info"))
            return LogLevel.INFO;
        else if (level.equals("warning"))
            return LogLevel.WARNING;
        else if (level.equals("error"))
            return LogLevel.ERROR;
        else if (level.equals("off"))
            return LogLevel.OFF;
        else
            return Assert.error();
    }
}
