/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.config.resource.ResourceNotFoundException;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.expression.Templates;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.config.AppenderConfiguration;
import com.exametrika.common.log.config.LoggerConfiguration;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Threads;



/**
 * The {@link LoggingService} is a simple logger factory implementation.
 *
 * @see ILogger
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class LoggingService implements ILoggingService, IService
{
    public static final LoggingService instance = new LoggingService();
    private volatile LoggingConfiguration configuration;
    private Map<String, IAppender> appenders = new LinkedHashMap<String, IAppender>();
    private Map<String, Logger> loggers = new LinkedHashMap<String, Logger>();
    private final TaskExecutor executor;
    private final TaskQueue<LogEvent> queue;
    private final CompileContext compileContext;
    private final ITemplateRegistry templateRegistry;
    private final LogContext context;
    private final Map<String, Object> runtimeContext;
    private final Stopper stopper = new Stopper();
    private volatile boolean stopped;
    private long lastFlushTime;

    public LoggingService()
    {
        this(loadDefaultConfiguration());
    }
    
    public LoggingService(LoggingConfiguration configuration)
    {
        Assert.notNull(configuration);
        
        TaskQueue<LogEvent> queue = new TaskQueue<LogEvent>(10000, 0);
        executor = new TaskExecutor<LogEvent>(1, queue, new RunnableTaskHandler<LogEvent>(), "Logging service handler thread", false, null);
        this.queue = queue;
        Set<String> qualifiers = Collections.singleton("logging");
        compileContext = Expressions.createCompileContext(qualifiers);
        this.context = new LogContext();
        this.runtimeContext = Expressions.createRuntimeContext(qualifiers, true);
        this.templateRegistry = createTemplateRegistry(qualifiers);

        executor.start();

        setConfiguration(configuration);
        
        Runtime.getRuntime().addShutdownHook(stopper);
    }
    
    public void addLog(LogEvent task)
    {
        LogInterceptor.INSTANCE.onLog(task);
        
        if (!stopped)
            queue.put(task);
    }
    
    @Override
    public synchronized ILogger createLogger(String name)
    {
        Logger logger = loggers.get(name);
        if (logger == null)
        {        
            logger = new Logger(this, createLoggerInfo(name));
            loggers.put(name, logger);
        }
        
        return logger;
    }

    @Override
    public IMarker createMarker(String name, IMarker[] references)
    {
        return new Marker(name, references);
    }

    @Override
    public LoggingConfiguration getConfiguration()
    {
        return configuration;
    }
    
    @Override
    public synchronized void onTimer(long currentTime)
    {
        if (lastFlushTime != 0 && currentTime < lastFlushTime + 1000)
            return;

        lastFlushTime = currentTime;
        
        flush();
    }

    @Override
    public void wire(IServiceRegistry registry)
    {
    }
    
    @Override
    public void start(IServiceRegistry registry)
    {
        Runtime.getRuntime().removeShutdownHook(stopper);
    }

    @Override
    public void stop(boolean fromShutdownHook)
    {
        Threads.sleep(1000);
        stopped = true;
        executor.stop();

        synchronized (this)
        {
            flush();
            
            setConfiguration(new LoggingConfiguration());
        }
        
        if (!fromShutdownHook)
            Runtime.getRuntime().removeShutdownHook(stopper);
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context)
    {
        LoggingConfiguration configuration = context.get(LoggingConfiguration.SCHEMA); 
        setConfiguration(configuration);
    }

    @Override
    public synchronized void setConfiguration(LoggingConfiguration configuration)
    {
        Assert.notNull(configuration);
        
        if (this.configuration != null && this.configuration.equals(configuration))
            return;
        
        for (Map.Entry<String, IAppender> entry : appenders.entrySet())
            entry.getValue().stop();
        
        appenders.clear();
        this.configuration = configuration;
        
        for (AppenderConfiguration appenderConfiguration : configuration.getAppenders())
            appenders.put(appenderConfiguration.getName(), appenderConfiguration.createAppender(compileContext, context, 
                runtimeContext, templateRegistry));
        
        for (Map.Entry<String, IAppender> entry : appenders.entrySet())
            entry.getValue().start();
        
        for (Map.Entry<String, Logger> entry : loggers.entrySet())
        {
            Logger logger = entry.getValue();
            logger.setLoggerInfo(createLoggerInfo(logger.getName()));
        }
    }

    private void flush()
    {
        for (Map.Entry<String, IAppender> entry : appenders.entrySet())
            entry.getValue().flush();
    }
    
    private LoggerInfo createLoggerInfo(String loggerName)
    {
        LoggerConfiguration loggerConfiguration = configuration.getLogger(loggerName);
        List<IAppender> appenders = new ArrayList<IAppender>(loggerConfiguration.getAppenders().size());
        
        for (String appenderName : loggerConfiguration.getAppenders())
        {
            IAppender appender = this.appenders.get(appenderName);
            Assert.notNull(appender);
            
            appenders.add(appender);
        }
        
        List<IExpression> compiledFilters = new ArrayList<IExpression>();
        for (String filter : loggerConfiguration.getFilters())
            compiledFilters.add(Expressions.compile(filter, compileContext));
        
        return new LoggerInfo(loggerConfiguration.getName(), loggerConfiguration.getLevel(), appenders, compiledFilters, 
            loggerConfiguration.isRecordStackTrace(), context, runtimeContext);
    }
    
    private static LoggingConfiguration loadDefaultConfiguration()
    {
        boolean debug = System.getenv("EXA_DEBUG") != null || System.getProperty("com.exametrika.debug", "false").equals("true");
        if (debug)
            return new LoggingConfiguration(LogLevel.TRACE);
        
        ConfigurationLoader loader = new ConfigurationLoader();
        try
        {
            return loader.loadConfiguration("classpath:logging.conf").get(LoggingConfiguration.SCHEMA);
        }
        catch (InvalidConfigurationException e)
        {
            if (!(e.getCause() instanceof ResourceNotFoundException))
                e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new LoggingConfiguration();
    }

    private ITemplateRegistry createTemplateRegistry(Set<String> qualifiers)
    {
        ITemplateRegistry templateRegistry = Templates.createTemplateRegistry(qualifiers);
        String defaultTemplate = "<%dateTime(s, s)%> <%color(highlight)%><%level%><%color(normal)%> [<%thread%>] <%logger(30)%>" + 
            "<%# if (!marker.isEmpty()) { %> [<%marker%>]<%# } %> -<%# if (!message.isEmpty()) { %> <%message%><%# } %> <%exception%>";
        String shortTemplate = "<%time(s)%> <%color(highlight)%><%truncate(level, 1)%><%color(normal)%> [<%truncate(thread, 5)%>] <%logger(1)%>" + 
            "<%# if (!marker.isEmpty()) { %> [<%truncate(marker, 5)%>]<%# } %> -<%# if (!message.isEmpty()) { %> <%message%><%# } %> <%exception(3)%>";
        String testTemplate = "<%relative(s)%> <%color(highlight)%><%level%><%color(normal)%> [<%thread%>] <%logger(30)%>" + 
            "<%# if (!marker.isEmpty()) { %> [<%marker%>]<%# } %> -<%# if (!message.isEmpty()) { %> <%message%><%# } %> <%rootException%>";
        templateRegistry.addTemplate("default", defaultTemplate);
        templateRegistry.addTemplate("short", shortTemplate);
        templateRegistry.addTemplate("test", testTemplate);
        return templateRegistry;
    }
    
    private class Stopper extends Thread
    {
        @Override
        public void run()
        {
            LoggingService.this.stop(true);
        }
    }
}
