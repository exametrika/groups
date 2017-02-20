/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.io.File;
import java.nio.ByteOrder;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawBatchOperation;
import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.impl.ChildResourceAllocator;
import com.exametrika.common.resource.impl.RootResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;



/**
 * The {@link RawDatabase} is an implementation of {@link IRawDatabase}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawDatabase implements IRawDatabase, ITimeService, ICompartmentTimerProcessor
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(RawDatabase.class);
    private volatile RawDatabaseConfiguration configuration;
    private volatile RawDatabaseConfiguration preparedConfiguration;
    private final RawFileCache fileCache;
    private final RawTransactionLog transactionLog;
    private final RawTransactionManager transactionManager;
    private final RawBatchManager batchManager;
    private final RawPageManager pageManager;
    private final RawPageTypeManager pageTypeManager;
    private final RawPagePool pagePool;
    private final ICompartment compartment;
    private final boolean compartmentOwner;
    private IResourceAllocator resourceAllocator;
    private final IResourceAllocator externalResourceAllocator;
    private final IMarker marker;
    private final RawTransactionQueue transactionQueue;
    private final RawPageProxyCache proxyCache;
    private volatile boolean started;
    private volatile boolean stopped;
    private volatile int interceptId;
    
    public RawDatabase(RawDatabaseConfiguration configuration, ICompartment compartment, boolean compartmentOwner, 
        IRawBatchContext batchContext, IResourceAllocator resourceAllocator)
    {
        Assert.notNull(configuration);
        Assert.notNull(compartment);
        Assert.isTrue(((Compartment)compartment).getQueue() instanceof RawTransactionQueue);
        
        Assert.checkState(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

        interceptId = RawDatabaseInterceptor.INSTANCE.onStarted(configuration.getName());

        this.configuration = configuration;
        this.preparedConfiguration = configuration;
        this.compartment = compartment;
        this.compartmentOwner = compartmentOwner;
        transactionQueue = (RawTransactionQueue)((Compartment)compartment).getQueue();
        marker = Loggers.getMarker(configuration.getName());
        
        this.resourceAllocator = configuration.getResourceAllocator().createAllocator();
        externalResourceAllocator = resourceAllocator;
        Assert.isTrue((this.resourceAllocator instanceof ChildResourceAllocator) == (externalResourceAllocator != null));
        
        fileCache = new RawFileCache(this);
        transactionManager = new RawTransactionManager(this, compartment);
        batchManager = new RawBatchManager(transactionManager, batchContext);
        transactionLog = new RawTransactionLog(new File(configuration.getPaths().get(0)), this);

        pageManager = new RawPageManager(this, configuration);
        pagePool = new RawPagePool(configuration.getPageTypes().get(0).getDefaultPageCategory().getMaxPageIdlePeriod());
        pageTypeManager = new RawPageTypeManager(pageManager, pagePool, this, configuration.getPageTypes(), 
            configuration.getFlags().contains(Flag.NATIVE_MEMORY), configuration.getTimerPeriod(), this.resourceAllocator);
        proxyCache = new RawPageProxyCache();
    }

    public RawFileCache getFileCache()
    {
        return fileCache;
    }
    
    public RawTransactionLog getTransactionLog()
    {
        return transactionLog;
    }

    public RawTransactionManager getTransactionManager()
    {
        return transactionManager;
    }
    
    public RawBatchManager getBatchManager()
    {
        return batchManager;
    }
    
    public RawPageManager getPageManager()
    {
        return pageManager;
    }
    
    public RawPageTypeManager getPageTypeManager()
    {
        return pageTypeManager;
    }
    
    public RawPagePool getPagePool()
    {
        return pagePool;
    }
    
    public ICompartment getCompartment()
    {
        return compartment;
    }
    
    public boolean isStopped()
    {
        return stopped;
    }
    
    public IResourceAllocator getResourceAllocator()
    {
        return resourceAllocator;
    }
    
    public RawPageProxyCache getProxyCache()
    {
        return proxyCache;
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public int getInterceptId()
    {
        return interceptId;
    }
    
    public void clearBatchCache()
    {
        batchManager.clearCache();
    }

    public void printStatistics()
    {
        IRawOperation operation = new RawOperation(true)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                StringBuilder builder = new StringBuilder();
                
                builder.append(fileCache.printStatistics());
                builder.append('\n');
                builder.append(pageManager.printStatistics());
                builder.append('\n');
                builder.append(pagePool.printStatistics());
                builder.append('\n');
                builder.append(pageTypeManager.printStatistics());
                builder.append("resource allocator:\n");
                builder.append(resourceAllocator.getStatistics().toString());
                
                String statistics = messages.statistics(configuration.toString()) + "\n" + Strings.indent(builder.toString(), 4);
                
                System.out.println(statistics);
            }
        };
        
        if (started && !stopped && !((Compartment)compartment).isMainThread())
            transactionSync(operation);
        else
            operation.run(null);
    }
    
    @Override
    public long getCurrentTime()
    {
        return compartment.getCurrentTime();
    }

    @Override
    public RawDatabaseConfiguration getConfiguration()
    {
        return configuration;
    }
    
    @Override
    public void setConfiguration(RawDatabaseConfiguration configuration)
    {
        Assert.checkState(started && !stopped);
        Assert.notNull(configuration);
        
        synchronized (this)
        {
            if (preparedConfiguration.equals(configuration))
                return;
            
            Assert.isTrue(preparedConfiguration.isCompatible(configuration));
            preparedConfiguration = configuration;
        }
        
        transactionSync(new RawOperation(IRawOperation.FLUSH)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                RawDatabaseConfiguration preparedConfiguration = RawDatabase.this.preparedConfiguration;
                if (preparedConfiguration == RawDatabase.this.configuration)
                    return;
                
                boolean clearCache = false;
                clearCache = setConfigurationInTransaction(preparedConfiguration, clearCache);
                
                if (clearCache)
                    throw new RawClearCacheException();
            }
        });
    }
    
    public boolean setConfigurationInTransaction(RawDatabaseConfiguration preparedConfiguration, boolean clearCache)
    {
        if (!preparedConfiguration.getResourceAllocator().equals(configuration.getResourceAllocator()))
        {
            IResourceAllocator resourceAllocator = preparedConfiguration.getResourceAllocator().createAllocator();
            Assert.isTrue((resourceAllocator instanceof ChildResourceAllocator) == (externalResourceAllocator != null));
            
            stopResourceAllocator();
            RawDatabase.this.resourceAllocator = resourceAllocator;
            startResourceAllocator();
            
            pageTypeManager.setResourceAllocator(resourceAllocator);
            clearCache = true;
        }
        
        if (preparedConfiguration.getTimerPeriod() != configuration.getTimerPeriod())
        {
            if (compartmentOwner)
                compartment.setDispatchPeriod(preparedConfiguration.getTimerPeriod());
            
            pageManager.setTimerPeriod(preparedConfiguration.getTimerPeriod());
        }
        
        this.configuration = preparedConfiguration;

        pageManager.setFlushPeriod(preparedConfiguration.getFlushPeriod());
        pageManager.setMaxFlushSize(preparedConfiguration.getMaxFlushSize());
        pagePool.setMaxPageIdlePeriod(preparedConfiguration.getPageTypes().get(0).getDefaultPageCategory().getMaxPageIdlePeriod());
        
        pageTypeManager.setConfiguration(preparedConfiguration, clearCache);
        return clearCache;
    }

    @Override
    public void onTimer(long currentTime)
    {
        pageManager.onTimer(currentTime);
        pageTypeManager.onTimer(currentTime);
        pagePool.onTimer(currentTime);
        
            RawDatabaseInterceptor.INSTANCE.onDatabase(interceptId, resourceAllocator.getStatistics(), fileCache.getFileCount(),
                pagePool.getSize(), transactionQueue.getCapacity());
    }

    @Override
    public void start()
    {
        synchronized (this)
        {
            if (started)
                return;
            
            started = true;
            
            for (String path : configuration.getPaths())
            {
                File file = new File(path);
                file.mkdirs();
                Assert.isTrue(file.isDirectory());
            }

            compartment.addTimerProcessor(this);
            
            transactionLog.open();
        }
        
        pageManager.start();
        
        if (compartmentOwner)
            compartment.start();
        
        if (batchManager.getBatchContext() != null)
            batchManager.getBatchContext().open();
        
        batchManager.open(configuration.getPaths().get(0));
        
        startResourceAllocator();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.databaseStarted(configuration.getPaths().toString()));
    }

    @Override
    public void stop()
    {
        stopResourceAllocator();
        
        if (compartmentOwner)
            compartment.stop();
        
        pageManager.stop();
        transactionManager.close();
        
        synchronized (this)
        {
            if (stopped)
                return;
            
            pageManager.flush(true);
            pageTypeManager.close();
            pageManager.close();
            pagePool.close();
            fileCache.close();
            transactionLog.close();
            batchManager.clearCache();

            compartment.removeTimerProcessor(this);
            
            stopped = true;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.databaseStopped(configuration.getPaths().toString()));
        }
        
        RawDatabaseInterceptor.INSTANCE.onStopped(interceptId);
    }

    @Override
    public void flush()
    {
        Assert.checkState(started && !stopped);
        
        transactionSync(new RawOperation(IRawOperation.FLUSH)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
            }
        });
    }

    @Override
    public void clearCaches()
    {
        Assert.checkState(started && !stopped);
        
        transactionSync(new RawOperation(IRawOperation.FLUSH)
        {
            @Override
            public void run(IRawTransaction transaction)
            {
                throw new RawClearCacheException();
            }
        });
    }
    
    @Override
    public void transaction(IRawOperation operation)
    {
        Assert.notNull(operation);
        Assert.checkState(started && !stopped);
         
        transactionManager.transaction(operation);
    }
    
    @Override
    public void transaction(List<IRawOperation> operations)
    {
        Assert.notNull(operations);
        Assert.checkState(started && !stopped);
         
        transactionManager.transaction(operations);
    }
    
    @Override
    public void transactionSync(IRawOperation operation)
    {
        Assert.notNull(operation);
        Assert.checkState(started && !stopped);
        Assert.checkState(!((Compartment)compartment).isMainThread());
         
        transactionManager.transactionSync(operation);
    }
    
    @Override
    public void transaction(IRawBatchOperation operation)
    {
        Assert.notNull(operation);
        Assert.checkState(started && !stopped);
         
        transactionManager.transaction(operation);
    }
    
    @Override
    public void transactionSync(IRawBatchOperation operation)
    {
        Assert.notNull(operation);
        Assert.checkState(started && !stopped);
        Assert.checkState(!((Compartment)compartment).isMainThread());
         
        transactionManager.transactionSync(operation);
    }

    @Override
    public String toString()
    {
        return configuration.toString();
    }
    
    @Override
    protected void finalize()
    {
        stop();
    }

    private void startResourceAllocator()
    {
        if (externalResourceAllocator != null)
        {
            ChildResourceAllocator childResourceAllocator = (ChildResourceAllocator)resourceAllocator;
            externalResourceAllocator.register(childResourceAllocator.getName(), childResourceAllocator);
        }
        else
            ((RootResourceAllocator)resourceAllocator).start();
    }
    
    private void stopResourceAllocator()
    {
        if (externalResourceAllocator != null)
        {
            ChildResourceAllocator childResourceAllocator = (ChildResourceAllocator)resourceAllocator;
            externalResourceAllocator.unregister(childResourceAllocator.getName());
        }
        else
            ((RootResourceAllocator)resourceAllocator).stop();
    }

    private interface IMessages
    {
        @DefaultMessage("Database ''{0}'' is opened.")
        ILocalizedMessage databaseStarted(String paths);
        @DefaultMessage("Database ''{0}'' is closed.")
        ILocalizedMessage databaseStopped(String paths);
        @DefaultMessage("Database ''{0}'' statistics:")
        ILocalizedMessage statistics(String paths);
    }
}
