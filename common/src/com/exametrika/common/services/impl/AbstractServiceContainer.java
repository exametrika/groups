/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.impl;

import java.io.File;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoaderFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.property.Properties;
import com.exametrika.common.config.property.SystemPropertyResolver;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.log.impl.ILoggingService;
import com.exametrika.common.log.impl.LoggingService;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.services.Services;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Times;



/**
 * The {@link AbstractServiceContainer} is an abstract service container.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public abstract class AbstractServiceContainer implements IServiceRegistry, ITimerListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractServiceContainer.class);
    protected final String name;
    protected final Map<String, Object> parameters;
    private LoggingService loggingService = LoggingService.instance;
    private volatile Map<String, IService> services = new LinkedHashMap<String, IService>();
    private long currentTime = Times.getCurrentTime();
    private Timer timer;
    private String configurationPath;

    public AbstractServiceContainer(String name, Map<String, Object> parameters)
    {
        Assert.notNull(name);
        Assert.notNull(parameters);
        
        this.name = name;
        this.parameters = parameters;
    }
    
    public String getConfigurationPath()
    {
        return configurationPath;
    }
    
    public final void start(String configurationPath)
    {
        loggingService.start(null);
        
        configurationPath = getConfigurationPath(configurationPath);
        ILoadContext context = null;
        if (configurationPath != null)
        {
            IConfigurationLoader loader = createConfigurationLoader();
            context = loader.loadConfiguration(configurationPath);
            
            loggingService.setConfiguration(context);
            this.configurationPath = configurationPath;
        }
        
        ServiceRegistrar registrar = new ServiceRegistrar();
        
        loadProviders(registrar);
        
        Map<String, IService> services = new LinkedHashMap<String, IService>(registrar.services);
        this.services = services;

        timer = new Timer(100, this, false, MessageFormat.format("Service container {0} timer thread", name), null);
        timer.start();

        for (IService service : services.values())
            service.wire(this);
        
        for (IService service : services.values())
            service.start(this);

        if (context != null)
        {
            for (IService service : services.values())
                service.setConfiguration(context);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started(name, configurationPath));
    }

    public final void stop(boolean fromShutdownHook)
    {
        if (timer != null)
        {
            timer.stop();
            timer = null;
        }

        for (IService service : services.values())
            service.stop(fromShutdownHook);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped(name));

        services = new LinkedHashMap<String, IService>();
        
        loggingService.stop(fromShutdownHook);
    }

    @Override
    public <T> T findParameter(String name)
    {
        return (T)parameters.get(name);
    }

    @Override
    public <T extends IService> T findService(String name)
    {
        Assert.notNull(name);
        if (name.equals(ILoggingService.NAME))
            return (T)loggingService;
        else
            return (T)services.get(name);
    }

    @Override
    public final void setConfiguration(String configurationPath)
    {
        configurationPath = getConfigurationPath(configurationPath);
        IConfigurationLoader loader = createConfigurationLoader();
        ILoadContext context = loader.loadConfiguration(configurationPath);
        this.configurationPath = configurationPath;
        
        Map<String, IService> services = this.services;
        
        loggingService.setConfiguration(context);
        
        for (IService service : services.values())
            service.setConfiguration(context);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationSet(name, configurationPath));
    }

    @Override
    public final void onTimer()
    {
        currentTime = Times.getCurrentTime();
        
        for (IService service : services.values())
            service.onTimer(currentTime);
        
        loggingService.onTimer(currentTime);
    }
    
    protected abstract void loadProviders(IServiceRegistrar registrar);
    
    private IConfigurationLoader createConfigurationLoader()
    {
        IConfigurationLoaderFactory configurationLoaderFactory = Services.loadProvider(IConfigurationLoaderFactory.class);
        if (configurationLoaderFactory != null)
            return configurationLoaderFactory.createLoader(parameters);
        else
            return new ConfigurationLoader(parameters);
    }
    
    private String getConfigurationPath(String configurationPath)
    {
        if (configurationPath != null && !configurationPath.isEmpty())
            return configurationPath;

        configurationPath = getPath(System.getProperty("com.exametrika.config"));
        if (configurationPath != null)
            return configurationPath;

        configurationPath = getPath(System.getenv("EXA_CONFIG"));
        if (configurationPath != null)
            return configurationPath;
        
        configurationPath = getPath("${user.home}/.exametrika/exametrika.conf" + File.pathSeparatorChar + 
            "${com.exametrika.home}/conf/exametrika.conf");
        if (configurationPath != null)
            return configurationPath;

        return null;
    }
    
    private String getPath(String pathsStr)
    {
        if (pathsStr == null)
            return null;
        
        SystemPropertyResolver resolver = new SystemPropertyResolver();
        String[] paths = pathsStr.split("[" + File.pathSeparatorChar + "]");
        for (String path : paths)
        {
            path = Properties.expandProperties(null, resolver, path, true, false);
            File file = new File(path);
            if (file.exists())
                return path.toString();
        }
        
        return null;
    }
    
    private class ServiceRegistrar implements IServiceRegistrar
    {
        private Map<String, IService> services = new LinkedHashMap<String, IService>();

        @Override
        public void register(String name, IService service)
        {
            Assert.notNull(name);
            Assert.notNull(service);
            
            if (services.containsKey(name))
                throw new InvalidArgumentException(messages.serviceAlreadyRegistered(name, AbstractServiceContainer.this.name));
            
            services.put(name, service);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.serviceRegistered(name, AbstractServiceContainer.this.name));
        }        
    }
    
    private interface IMessages
    {
        @DefaultMessage("Service container ''{0}'' is stopped.")
        ILocalizedMessage stopped(String name);

        @DefaultMessage("Service ''{0}'' is already registered in service container ''{1}''.")
        ILocalizedMessage serviceAlreadyRegistered(String name, String containerName);

        @DefaultMessage("Service container ''{0}'' is started with configuration ''{1}''.")
        ILocalizedMessage started(String name, String configurationPath);
        
        @DefaultMessage("Service ''{0}'' is registered in service container ''{1}''.")
        ILocalizedMessage serviceRegistered(String name, String containerName);
        
        @DefaultMessage("Configuration ''{0}'' of service container ''{1}'' is set.")
        ILocalizedMessage configurationSet(String configurationPath, String containerName);
    }
}
