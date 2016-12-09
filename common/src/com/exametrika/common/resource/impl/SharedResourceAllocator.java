/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;

/**
 * The {@link SharedResourceAllocator} is a top level implementation of {@link IResourceAllocator} which allocates full resource
 * to its consumers residing in different processes of the same host.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SharedResourceAllocator extends RootResourceAllocator
{
    private static final ILogger logger = Loggers.get(SharedResourceAllocator.class);
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final Object sync = new Object();
    private final String dataExchangeFileName;
    private final long dataExchangePeriod;
    private final long staleAllocatorPeriod;
    private final long initialQuota;
    private RandomAccessFile dataExchangeFile;
    private Map<String, ExternalResourceAllocator> externalAllocators = new LinkedHashMap<String, ExternalResourceAllocator>();
    private long lastDataExchangeTime;
    private long nextAllocationTime;
    private boolean dataExchanged;

    public SharedResourceAllocator(String dataExchangeFileName, String name, IResourceProvider resourceProvider, 
        Map<String, IAllocationPolicy> policies, IAllocationPolicy defaultPolicy,  
        long timerPeriod, long allocationPeriod, long dataExchangePeriod, long staleAllocatorPeriod, long initialQuota,
        long quotaIncreaseDelay, long initializePeriod, ITimeService timeService)
    {
        super(name, resourceProvider, policies, defaultPolicy, timerPeriod, allocationPeriod, quotaIncreaseDelay, 
            initializePeriod, timeService);
        
        Assert.notNull(dataExchangeFileName);
        
        this.dataExchangeFileName = dataExchangeFileName;
        this.dataExchangePeriod = dataExchangePeriod;
        this.staleAllocatorPeriod = staleAllocatorPeriod;
        this.initialQuota = initialQuota;
    }
    
    @Override
    public void start()
    {
        try
        {
            dataExchangeFile = new RandomAccessFile(dataExchangeFileName, "rw");
        }
        catch (IOException e)
        {
            IOs.close(dataExchangeFile);
            Exceptions.wrapAndThrow(e);
        }
        
        super.start();
    }

    @Override
    public void stop()
    {
        super.stop();
        
        exchangeData(true);
        IOs.close(dataExchangeFile);
    }
    
    @Override
    public void onTimer()
    {
        long currentTime = timeService.getCurrentTime();
        
        if (lastDataExchangeTime == 0 || currentTime > lastDataExchangeTime + dataExchangePeriod)
        {
            exchangeData(false);
            lastDataExchangeTime = currentTime;
        }
        
        if (currentTime > nextAllocationTime)
        {
            if (dataExchanged)
                allocate();
            else
                setQuota(initialQuota);
            
            nextAllocationTime = (currentTime / allocationPeriod + 1) * allocationPeriod;
        }
        
        doOnTimer();
    }

    @Override
    protected String[] parseName(String name)
    {
        return super.parseName(this.name + "." + name);
    }

    private synchronized void exchangeData(boolean close)
    {
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.startExchangeData(dataExchangeFileName));
        
        synchronized (sync)
        {
            FileLock lock = null;
            try
            {
                lock = dataExchangeFile.getChannel().lock();
                update(close);
            }
            catch (IOException e)
            {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
            finally
            {
                IOs.close(lock);
            }
        }
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.finishExchangeData(dataExchangeFileName));
        
        dataExchanged = true;
    }
    
    private void update(boolean close) throws IOException
    {
        long currentTime = timeService.getCurrentTime();

        if (dataExchangeFile.length() > 0)
        {
            try
            {
                dataExchangeFile.seek(0);
                int count = dataExchangeFile.readInt();
                Map<String, ExternalResourceAllocator> externalAllocators = new LinkedHashMap<String, ExternalResourceAllocator>(count);
                    
                for (int i = 0; i < count; i++)
                {
                    String name = dataExchangeFile.readUTF();
                    long time = dataExchangeFile.readLong();
                    long amount = dataExchangeFile.readLong();
                    
                    if (this.name.equals(name))
                        continue;
                    if (currentTime > time + staleAllocatorPeriod)
                        continue;
                    
                    externalAllocators.put(name, new ExternalResourceAllocator(name, time, amount));
                }
    
                if (!close)
                {
                    for (Iterator<ExternalResourceAllocator> it = this.externalAllocators.values().iterator(); it.hasNext(); )
                    {
                        ExternalResourceAllocator externalAllocator = it.next();
                        if (!externalAllocators.containsKey(externalAllocator.name))
                        {
                            unregister(externalAllocator.name);
                            it.remove();
                            
                            if (logger.isLogEnabled(LogLevel.DEBUG))
                                logger.log(LogLevel.DEBUG, marker, messages.externalAllocatorUnregistered(externalAllocator.name));
                        }
                    }
                    
                    for (ExternalResourceAllocator externalAllocator : externalAllocators.values())
                    {
                        ExternalResourceAllocator existing = this.externalAllocators.get(externalAllocator.name);
                        if (existing != null)
                        {
                            existing.time = externalAllocator.time;
                            existing.amount = externalAllocator.amount;
                        }
                        else
                        {
                            this.externalAllocators.put(externalAllocator.name, externalAllocator);
                            register(externalAllocator.name, externalAllocator);
                            
                            if (logger.isLogEnabled(LogLevel.DEBUG))
                                logger.log(LogLevel.DEBUG, marker, messages.externalAllocatorRegistered(externalAllocator.name));
                        }
                    }
                }
            }
            catch (IOException e)
            {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }

        dataExchangeFile.seek(0);
        dataExchangeFile.writeInt(externalAllocators.size() + (close ? 0 : 1));
        
        long externalAmount = 0;
        for (ExternalResourceAllocator externalAllocator : externalAllocators.values())
        {
            dataExchangeFile.writeUTF(externalAllocator.name);
            dataExchangeFile.writeLong(externalAllocator.time);
            dataExchangeFile.writeLong(externalAllocator.amount);
            externalAmount += externalAllocator.amount;
        }
        
        if (!close)
        {
            dataExchangeFile.writeUTF(name);
            dataExchangeFile.writeLong(currentTime);
            dataExchangeFile.writeLong(getAmount() - externalAmount);
        }
    }
    
    private static class ExternalResourceAllocator implements IResourceConsumer
    {
        private final String name;
        private long time;
        private long amount;
        private long quota;
        
        public ExternalResourceAllocator(String name, long time, long amount)
        {
            Assert.notNull(name);
            
            this.name = name;
            this.time = time;
            this.amount = amount;
        }

        @Override
        public long getAmount()
        {
            return amount;
        }

        @Override
        public long getQuota()
        {
            return quota;
        }

        @Override
        public void setQuota(long value)
        {
            quota = value;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof ExternalResourceAllocator))
                return false;
            
            ExternalResourceAllocator allocator = (ExternalResourceAllocator)o;
            return name.equals(allocator.name);
        }
        
        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("External allocator ''{0}'' has been registered.")
        ILocalizedMessage externalAllocatorRegistered(String name);

        @DefaultMessage("External allocator ''{0}'' has been unregistered.")
        ILocalizedMessage externalAllocatorUnregistered(String name);

        @DefaultMessage("Data exchange with ''{0}'' has been finished.")
        ILocalizedMessage finishExchangeData(String dataExchangeFileName);

        @DefaultMessage("Data exchange with ''{0}'' has been started.")
        ILocalizedMessage startExchangeData(String dataExchangeFileName);
    }
}
