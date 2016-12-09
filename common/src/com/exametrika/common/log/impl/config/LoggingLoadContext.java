/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.common.CommonConfiguration;
import com.exametrika.common.config.common.ICommonLoadContext;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.utils.Assert;





/**
 * The {@link LoggingLoadContext} is a helper class that is used to load {@link LoggingConfiguration}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class LoggingLoadContext implements ILoggingLoadContext, IContextFactory, IConfigurationFactory
{
    private Map<String, AppenderConfiguration> appenders = new LinkedHashMap<String, AppenderConfiguration>();
    private LoggerConfiguration rootLogger = new LoggerConfiguration();
    private Map<String, LoggerConfiguration> loggers = new LinkedHashMap<String, LoggerConfiguration>();
    
    public void setRootLogger(LoggerConfiguration rootLogger)
    {
        Assert.notNull(rootLogger);
        
        this.rootLogger = rootLogger;
    }
    
    @Override
    public void addAppender(AppenderConfiguration appender)
    {
        Assert.notNull(appender);
        
        appenders.put(appender.getName(), appender);
    }
    
    @Override
    public void addLogger(LoggerConfiguration logger)
    {
        Assert.notNull(logger);
        
        loggers.put(logger.getName(), logger);
    }

    @Override
    public LoggingConfiguration createConfiguration(ILoadContext context)
    {
        ICommonLoadContext commonContext = context.get(CommonConfiguration.SCHEMA);
        return new LoggingConfiguration(commonContext.getRuntimeMode(), new ArrayList<AppenderConfiguration>(appenders.values()), 
            rootLogger, new ArrayList<LoggerConfiguration>(loggers.values()));
    }
    
    @Override
    public IConfigurationFactory createContext()
    {
        return new LoggingLoadContext();
    }
}
