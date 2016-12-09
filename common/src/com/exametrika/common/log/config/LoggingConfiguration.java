/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.config.ConsoleAppenderConfiguration.Target;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;



/**
 * The {@link LoggingConfiguration} is logging configuration.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class LoggingConfiguration extends Configuration
{
    public static final String SCHEMA = "com.exametrika.logging-1.0";
    
    private final RuntimeMode runtimeMode;
    private final LoggerConfiguration rootLogger;
    private final List<LoggerConfiguration> loggers;
    private final List<AppenderConfiguration> appenders;

    public LoggingConfiguration()
    {
        this(RuntimeMode.DEVELOPMENT, Arrays.<AppenderConfiguration>asList(new ConsoleAppenderConfiguration("console", LogLevel.TRACE, 
            "<%@template(default)%>", Collections.<String>emptyList(), Target.OUTPUT, false)), 
            new LoggerConfiguration("", LogLevel.INFO, Arrays.<String>asList("console"), 
                Collections.<String>emptyList(), false), Collections.<LoggerConfiguration>emptyList());
    }
    
    public LoggingConfiguration(LogLevel level)
    {
        this(RuntimeMode.DEVELOPMENT, Arrays.<AppenderConfiguration>asList(new ConsoleAppenderConfiguration("console", LogLevel.TRACE, 
            "<%@template(default)%>", Collections.<String>emptyList(), Target.OUTPUT, false)), 
            new LoggerConfiguration("", level, Arrays.<String>asList("console"), 
                Collections.<String>emptyList(), false), Collections.<LoggerConfiguration>emptyList());
    }
    
    public LoggingConfiguration(RuntimeMode runtimeMode, List<AppenderConfiguration> appenders, 
        LoggerConfiguration rootLogger, List<LoggerConfiguration> loggers)
    {
        Assert.notNull(runtimeMode);
        Assert.notNull(appenders);
        Assert.notNull(rootLogger);
        Assert.notNull(loggers);
        
        Set<String> set = new HashSet<String>();
        for (AppenderConfiguration appender : appenders)
            Assert.isTrue(set.add(appender.getName()));
        set.clear();
        for (LoggerConfiguration logger : loggers)
            Assert.isTrue(set.add(logger.getName()));

        this.runtimeMode = runtimeMode;
        this.rootLogger = rootLogger;
        this.loggers = Immutables.wrap(loggers);
        this.appenders = Immutables.wrap(appenders);
        checkAppenders(appenders, rootLogger.getAppenders());
        for (LoggerConfiguration logger : loggers)
            checkAppenders(appenders, logger.getAppenders());
    }
    
    public RuntimeMode getRuntimeMode()
    {
        return runtimeMode;
    }
    
    public LoggerConfiguration getRootLogger()
    {
        return rootLogger;
    }
    
    public List<LoggerConfiguration> getLoggers()
    {
        return loggers;
    }
    
    public List<AppenderConfiguration> getAppenders()
    {
        return appenders;
    }
    
    public LoggerConfiguration getLogger(String name)
    {
        LogLevel level = rootLogger.getLevel();
        List<String> appenders = rootLogger.getAppenders();
        List<String> filters = rootLogger.getFilters();
        String levelName = "";
        String appendersName = "";
        String filtersName = "";
        boolean recordStackTrace = rootLogger.isRecordStackTrace();
        
        for (LoggerConfiguration logger : loggers)
        {
            if ((name + ".").startsWith(logger.getName() + "."))
            {
                if (levelName.length() < logger.getName().length())
                {
                    levelName = logger.getName();
                    level = logger.getLevel();
                    recordStackTrace = logger.isRecordStackTrace();
                }
                
                if (!logger.getAppenders().isEmpty() && appendersName.length() < logger.getName().length())
                {
                    appendersName = logger.getName();
                    appenders = logger.getAppenders();
                }
                
                if (!logger.getFilters().isEmpty() && filtersName.length() < logger.getName().length())
                {
                    filtersName = logger.getName();
                    filters = logger.getFilters();
                }
            }
        }
        
        return new LoggerConfiguration(name, level, appenders, filters, recordStackTrace);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof LoggingConfiguration))
            return false;
        
        LoggingConfiguration configuration = (LoggingConfiguration)o;
        return runtimeMode.equals(configuration.runtimeMode) && appenders.equals(configuration.appenders) && 
            rootLogger.equals(configuration.rootLogger) && loggers.equals(configuration.loggers);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(runtimeMode, appenders, rootLogger, loggers);
    }
    
    private void checkAppenders(List<AppenderConfiguration> appenders, List<String> appenderNames)
    {
        for (String appenderName : appenderNames)
        {
            boolean found = false;
            
            for (AppenderConfiguration appender : appenders)
            {
                if (appender.getName().equals(appenderName))
                {
                    found = true;
                    break;
                }
            }
            
            Assert.isTrue(found);
        }
    }
}
